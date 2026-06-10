//  Copyright 2026 Yurun Zi
//  SPDX-License-Identifier: GPL-3.0-only
//  Additional permission under GPLv3 section 7 applies; see the LICENSE file.

#include "io/image/image_writer.hpp"

#include <OpenImageIO/imageio.h>
#include <gtest/gtest.h>

#include <exiv2/exiv2.hpp>

#include <filesystem>
#include <memory>
#include <opencv2/imgcodecs.hpp>
#include <vector>

#include "image/image_buffer.hpp"
#include "image/metadata.hpp"

namespace alcedo {
namespace {

class ImageWriterTests : public ::testing::Test {
 protected:
  std::filesystem::path temp_dir_;

  void SetUp() override {
    temp_dir_ = std::filesystem::temp_directory_path() / "alcedo_image_writer_test";
    std::filesystem::create_directories(temp_dir_);
    Exiv2::LogMsg::setLevel(Exiv2::LogMsg::Level::mute);
  }

  void TearDown() override {
    std::error_code ec;
    std::filesystem::remove_all(temp_dir_, ec);
  }
};

auto MakeColorProfile(ColorUtils::ColorSpace color_space, ColorUtils::EOTF eotf)
    -> ExportColorProfileConfig {
  return ExportColorProfileConfig{color_space, eotf, 600.0f};
}

auto ReadXmpRating(const std::filesystem::path& path) -> int {
  auto image = Exiv2::ImageFactory::open(path.string());
  if (!image) {
    return -1;
  }
  image->readMetadata();
  const auto& xmp_data = image->xmpData();
  const auto  rating   = xmp_data.findKey(Exiv2::XmpKey("Xmp.xmp.Rating"));
  return rating == xmp_data.end() ? -1 : static_cast<int>(rating->toInt64());
}

auto ReadExifRating(const std::filesystem::path& path) -> int {
  auto image = Exiv2::ImageFactory::open(path.string());
  if (!image) {
    return -1;
  }
  image->readMetadata();
  const auto& exif_data = image->exifData();
  const auto  rating    = exif_data.findKey(Exiv2::ExifKey("Exif.Image.Rating"));
  return rating == exif_data.end() ? -1 : static_cast<int>(rating->toInt64());
}

auto ReadExifString(const std::filesystem::path& path, const char* key) -> std::string {
  auto image = Exiv2::ImageFactory::open(path.string());
  if (!image) {
    return {};
  }
  image->readMetadata();
  const auto& exif_data = image->exifData();
  const auto  it        = exif_data.findKey(Exiv2::ExifKey(key));
  return it == exif_data.end() ? std::string{} : it->toString();
}

auto ReadXmpString(const std::filesystem::path& path, const char* key) -> std::string {
  auto image = Exiv2::ImageFactory::open(path.string());
  if (!image) {
    return {};
  }
  image->readMetadata();
  const auto& xmp_data = image->xmpData();
  const auto  it       = xmp_data.findKey(Exiv2::XmpKey(key));
  return it == xmp_data.end() ? std::string{} : it->toString();
}

void WriteTestJpeg(const std::filesystem::path& path, const std::vector<uint8_t>& rgb,
                   int width, int height) {
  OIIO_NAMESPACE_USING

  ImageSpec spec(width, height, 3, TypeDesc::UINT8);
  spec.channelnames = {"R", "G", "B"};
  std::unique_ptr<ImageOutput> output = ImageOutput::create(path.string());
  ASSERT_TRUE(output != nullptr);
  ASSERT_TRUE(output->open(path.string(), spec));
  ASSERT_TRUE(output->write_image(TypeDesc::UINT8, rgb.data()));
  ASSERT_TRUE(output->close());
}

}  // namespace

TEST_F(ImageWriterTests, UltraHdrTriggerMatchesHdrJpegCombinations) {
  ExportFormatOptions jpeg_options;
  jpeg_options.format_ = ImageFormatType::JPEG;

  EXPECT_TRUE(ImageWriter::ShouldWriteUltraHdr(
      jpeg_options, MakeColorProfile(ColorUtils::ColorSpace::REC2020, ColorUtils::EOTF::ST2084)));
  EXPECT_TRUE(ImageWriter::ShouldWriteUltraHdr(
      jpeg_options, MakeColorProfile(ColorUtils::ColorSpace::REC2020, ColorUtils::EOTF::HLG)));
  EXPECT_FALSE(ImageWriter::ShouldWriteUltraHdr(
      jpeg_options,
      MakeColorProfile(ColorUtils::ColorSpace::REC709, ColorUtils::EOTF::GAMMA_2_2)));

  jpeg_options.hdr_export_mode_ = ExportFormatOptions::HDR_EXPORT_MODE::EMBEDDED_PROFILE_ONLY;
  EXPECT_FALSE(ImageWriter::ShouldWriteUltraHdr(
      jpeg_options, MakeColorProfile(ColorUtils::ColorSpace::REC2020, ColorUtils::EOTF::ST2084)));

  ExportFormatOptions png_options;
  png_options.format_ = ImageFormatType::PNG;
  EXPECT_FALSE(ImageWriter::ShouldWriteUltraHdr(
      png_options, MakeColorProfile(ColorUtils::ColorSpace::REC2020, ColorUtils::EOTF::ST2084)));

  ExportFormatOptions tiff_options;
  tiff_options.format_ = ImageFormatType::TIFF;
  EXPECT_FALSE(ImageWriter::ShouldWriteUltraHdr(
      tiff_options, MakeColorProfile(ColorUtils::ColorSpace::REC2020, ColorUtils::EOTF::HLG)));

  ExportFormatOptions exr_options;
  exr_options.format_ = ImageFormatType::EXR;
  EXPECT_FALSE(ImageWriter::ShouldWriteUltraHdr(
      exr_options, MakeColorProfile(ColorUtils::ColorSpace::REC2020, ColorUtils::EOTF::ST2084)));
}

TEST_F(ImageWriterTests, LegacyJpegExportForcesUprightOrientation) {
  const auto src_path = temp_dir_ / "source.jpg";
  const auto dst_path = temp_dir_ / "exported.jpg";

  WriteTestJpeg(src_path, {255, 0, 0, 0, 255, 0}, 2, 1);

  {
    auto image = Exiv2::ImageFactory::open(src_path.string());
    ASSERT_TRUE(image != nullptr);
    image->readMetadata();
    Exiv2::ExifData exif_data = image->exifData();
    exif_data["Exif.Image.Orientation"] = static_cast<uint16_t>(6);
    image->setExifData(exif_data);
    image->writeMetadata();
  }

  cv::Mat rgba32f(1, 2, CV_32FC4);
  rgba32f.at<cv::Vec4f>(0, 0) = cv::Vec4f(1.0f, 0.0f, 0.0f, 1.0f);
  rgba32f.at<cv::Vec4f>(0, 1) = cv::Vec4f(0.0f, 1.0f, 0.0f, 1.0f);

  auto image_data = std::make_shared<ImageBuffer>(std::move(rgba32f));

  ExportFormatOptions options;
  options.format_ = ImageFormatType::JPEG;
  options.export_path_ = dst_path;

  ImageWriter::WriteImageToPath(
      src_path, image_data, options,
      ExportColorProfileConfig{ColorUtils::ColorSpace::REC709, ColorUtils::EOTF::GAMMA_2_2,
                               100.0f});

  ASSERT_TRUE(std::filesystem::exists(dst_path));

  auto output = Exiv2::ImageFactory::open(dst_path.string());
  ASSERT_TRUE(output != nullptr);
  output->readMetadata();

  const Exiv2::ExifData& exif_data = output->exifData();
  const auto orientation = exif_data.findKey(Exiv2::ExifKey("Exif.Image.Orientation"));
  if (orientation != exif_data.end()) {
    EXPECT_EQ(orientation->toString(), "1");
  }

  {
    OIIO_NAMESPACE_USING
    auto decoded = ImageInput::open(dst_path.string());
    ASSERT_TRUE(decoded != nullptr);
    const auto& spec = decoded->spec();
    EXPECT_EQ(spec.width, 2);
    EXPECT_EQ(spec.height, 1);
    decoded->close();
  }
}

TEST_F(ImageWriterTests, EmbeddedHdrIccModeRejectsHdrJpegExport) {
  const auto src_path = temp_dir_ / "hdr_source.jpg";
  const auto dst_path = temp_dir_ / "embedded_hdr.jpg";

  WriteTestJpeg(src_path, {
                           144, 96, 48, 144, 96, 48,
                           144, 96, 48, 144, 96, 48,
                         }, 2, 2);

  cv::Mat rgba32f(2, 2, CV_32FC4, cv::Scalar(0.65f, 0.35f, 0.15f, 1.0f));
  auto    image_data = std::make_shared<ImageBuffer>(std::move(rgba32f));

  ExportFormatOptions options;
  options.format_ = ImageFormatType::JPEG;
  options.export_path_ = dst_path;
  options.hdr_export_mode_ = ExportFormatOptions::HDR_EXPORT_MODE::EMBEDDED_PROFILE_ONLY;

  const auto hdr_profile =
      MakeColorProfile(ColorUtils::ColorSpace::REC2020, ColorUtils::EOTF::ST2084);

  EXPECT_THROW(ImageWriter::WriteImageToPath(src_path, image_data, options, hdr_profile),
               std::runtime_error);
  EXPECT_FALSE(std::filesystem::exists(dst_path));
}

TEST_F(ImageWriterTests, EmbeddedHdrIccModeRejectsMetadataInjectedHdrJpegExport) {
  const auto src_path = temp_dir_ / "hdr_metadata_source.jpg";
  const auto dst_path = temp_dir_ / "embedded_hdr_metadata.jpg";

  WriteTestJpeg(src_path, {
                           64, 32, 16, 64, 32, 16,
                           64, 32, 16, 64, 32, 16,
                         }, 2, 2);

  {
    auto image = Exiv2::ImageFactory::open(src_path.string());
    ASSERT_TRUE(image != nullptr);
    image->readMetadata();
    Exiv2::ExifData exif_data = image->exifData();
    exif_data["Exif.Photo.ColorSpace"] = static_cast<uint16_t>(1);
    image->setExifData(exif_data);
    image->writeMetadata();
  }

  cv::Mat rgba32f(2, 2, CV_32FC4, cv::Scalar(0.62f, 0.41f, 0.21f, 1.0f));
  auto    image_data = std::make_shared<ImageBuffer>(std::move(rgba32f));

  ExportFormatOptions options;
  options.format_ = ImageFormatType::JPEG;
  options.export_path_ = dst_path;
  options.hdr_export_mode_ = ExportFormatOptions::HDR_EXPORT_MODE::EMBEDDED_PROFILE_ONLY;

  const auto hdr_profile =
      MakeColorProfile(ColorUtils::ColorSpace::REC2020, ColorUtils::EOTF::ST2084);

  ExifDisplayMetaData metadata;
  metadata.lens_ = "Alcedo Test 35mm F1.8";
  metadata.date_time_str_ = "2024-05-06 07:08:09";
  metadata.rating_ = 5;

  EXPECT_THROW(ImageWriter::WriteImageToPath(src_path, image_data, options, hdr_profile, metadata),
               std::runtime_error);
  EXPECT_FALSE(std::filesystem::exists(dst_path));
}

TEST_F(ImageWriterTests, ExportWritesCurrentRatingMetadata) {
  const auto src_path = temp_dir_ / "rating_source.jpg";
  const auto dst_path = temp_dir_ / "rating_exported.jpg";

  WriteTestJpeg(src_path, {32, 64, 96, 96, 64, 32}, 2, 1);

  {
    auto image = Exiv2::ImageFactory::open(src_path.string());
    ASSERT_TRUE(image != nullptr);
    image->readMetadata();
    Exiv2::XmpData xmp_data = image->xmpData();
    xmp_data["Xmp.xmp.Rating"] = 1;
    image->setXmpData(xmp_data);
    Exiv2::ExifData exif_data = image->exifData();
    exif_data["Exif.Image.Rating"] = static_cast<uint16_t>(1);
    image->setExifData(exif_data);
    image->writeMetadata();
  }

  cv::Mat rgba32f(1, 2, CV_32FC4);
  rgba32f.at<cv::Vec4f>(0, 0) = cv::Vec4f(0.2f, 0.4f, 0.6f, 1.0f);
  rgba32f.at<cv::Vec4f>(0, 1) = cv::Vec4f(0.6f, 0.4f, 0.2f, 1.0f);
  auto image_data = std::make_shared<ImageBuffer>(std::move(rgba32f));

  ExportFormatOptions options;
  options.format_ = ImageFormatType::JPEG;
  options.export_path_ = dst_path;

  ExifDisplayMetaData metadata;
  metadata.rating_ = 4;

  ImageWriter::WriteImageToPath(src_path, image_data, options, std::nullopt, metadata);

  ASSERT_TRUE(std::filesystem::exists(dst_path));
  EXPECT_EQ(ReadExifRating(dst_path), 4);
  const int xmp_rating = ReadXmpRating(dst_path);
  if (xmp_rating >= 0) {
    EXPECT_EQ(xmp_rating, 4);
  }
}

TEST_F(ImageWriterTests, ExportWritesCurrentLensAndCaptureDateMetadata) {
  const auto src_path = temp_dir_ / "metadata_source.jpg";
  const auto dst_path = temp_dir_ / "metadata_exported.jpg";

  WriteTestJpeg(src_path, {128, 96, 64, 64, 96, 128}, 2, 1);

  cv::Mat rgba32f(1, 2, CV_32FC4);
  rgba32f.at<cv::Vec4f>(0, 0) = cv::Vec4f(0.5f, 0.4f, 0.3f, 1.0f);
  rgba32f.at<cv::Vec4f>(0, 1) = cv::Vec4f(0.3f, 0.4f, 0.5f, 1.0f);
  auto image_data = std::make_shared<ImageBuffer>(std::move(rgba32f));

  ExportFormatOptions options;
  options.format_ = ImageFormatType::JPEG;
  options.export_path_ = dst_path;

  ExifDisplayMetaData metadata;
  metadata.make_ = "AlcedoCam";
  metadata.model_ = "Model T";
  metadata.lens_make_ = "Alcedo Optics";
  metadata.lens_ = "Alcedo Optics 50mm F2";
  metadata.date_time_str_ = "2023-12-31 23:59:58";
  metadata.focal_ = 50.0f;
  metadata.aperture_ = 2.0f;
  metadata.iso_ = 400;

  ImageWriter::WriteImageToPath(src_path, image_data, options, std::nullopt, metadata);

  ASSERT_TRUE(std::filesystem::exists(dst_path));
  EXPECT_EQ(ReadExifString(dst_path, "Exif.Image.Make"), metadata.make_);
  EXPECT_EQ(ReadExifString(dst_path, "Exif.Image.Model"), metadata.model_);
  EXPECT_EQ(ReadExifString(dst_path, "Exif.Photo.LensMake"), metadata.lens_make_);
  EXPECT_EQ(ReadExifString(dst_path, "Exif.Photo.LensModel"), metadata.lens_);
  EXPECT_EQ(ReadExifString(dst_path, "Exif.Photo.DateTimeOriginal"), "2023:12:31 23:59:58");
  const auto xmp_create_date = ReadXmpString(dst_path, "Xmp.xmp.CreateDate");
  if (!xmp_create_date.empty()) {
    EXPECT_EQ(xmp_create_date, "2023-12-31T23:59:58");
  }
}

}  // namespace alcedo
