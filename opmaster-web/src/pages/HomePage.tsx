import HeroSection from '../components/home/HeroSection';
import AndroidDevicePreview from '../components/home/AndroidDevicePreview';
import AndroidFeatures from '../components/home/AndroidFeatures';

export default function HomePage() {
  return (
    <div className="min-h-screen">
      <HeroSection />
      <AndroidDevicePreview />
      <AndroidFeatures />
    </div>
  );
}
