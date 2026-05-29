import OppoHero from '../components/home/OppoHero';
import OppoQuickActions from '../components/home/OppoQuickActions';
import OppoFeaturedPresets from '../components/home/OppoFeaturedPresets';

export default function HomePage() {
  return (
    <div className="min-h-screen">
      <OppoHero />
      <OppoQuickActions />
      <OppoFeaturedPresets />
    </div>
  );
}
