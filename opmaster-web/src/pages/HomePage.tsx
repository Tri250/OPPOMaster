import HeroSection from '../components/home/HeroSection';
import PresetGrid from '../components/home/PresetGrid';
import AIDemoBanner from '../components/home/AIDemoBanner';
import FeaturesOverview from '../components/home/FeaturesOverview';

export default function HomePage() {
  return (
    <div className="min-h-screen">
      <HeroSection />
      <FeaturesOverview />
      <PresetGrid />
      <AIDemoBanner />
    </div>
  );
}
