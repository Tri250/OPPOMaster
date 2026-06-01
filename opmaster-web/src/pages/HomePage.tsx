import HeroSection from '../components/home/HeroSection';
import PresetGrid from '../components/home/PresetGrid';
import AIDemoBanner from '../components/home/AIDemoBanner';
import FeaturesOverview from '../components/home/FeaturesOverview';
import AndroidDevicePreview from '../components/home/AndroidDevicePreview';

export default function HomePage() {
  return (
    <div className="min-h-screen">
      <HeroSection />
      <AndroidDevicePreview />
      <FeaturesOverview />
      <PresetGrid />
      <AIDemoBanner />
    </div>
  );
}
