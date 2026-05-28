import HeroSection from '../components/home/HeroSection';
import FeaturesSection from '../components/home/FeaturesSection';
import PresetGrid from '../components/home/PresetGrid';
import AIDemoBanner from '../components/home/AIDemoBanner';

export default function HomePage() {
  return (
    <div className="min-h-screen">
      <HeroSection />
      <FeaturesSection />
      <PresetGrid />
      <AIDemoBanner />
    </div>
  );
}
