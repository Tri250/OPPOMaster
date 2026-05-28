import HeroSection from '../components/home/HeroSection';
import PresetGrid from '../components/home/PresetGrid';
import AIDemoBanner from '../components/home/AIDemoBanner';

export default function HomePage() {
  return (
    <div className="min-h-screen">
      <HeroSection />
      <PresetGrid />
      <AIDemoBanner />
    </div>
  );
}
