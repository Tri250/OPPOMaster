import Hero from './sections/Hero'
import Features from './sections/Features'
import FeatureShowcase from './sections/FeatureShowcase'
import MoreFeatures from './sections/MoreFeatures'
import Gallery from './sections/Gallery'
import Preview from './sections/Preview'
import Testimonials from './sections/Testimonials'
import Download from './sections/Download'
import QRScanner from './sections/QRScanner'

function App() {
  return (
    <div className="min-h-screen bg-[#0D1117]">
      <Hero />
      <Features />
      <FeatureShowcase />
      <MoreFeatures />
      <Gallery />
      <Preview />
      <Testimonials />
      <Download />
      <QRScanner />
    </div>
  )
}

export default App
