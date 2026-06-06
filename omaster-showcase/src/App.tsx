import Hero from './sections/Hero'
import Features from './sections/Features'
import Gallery from './sections/Gallery'
import Preview from './sections/Preview'
import Testimonials from './sections/Testimonials'
import Download from './sections/Download'

function App() {
  return (
    <div className="min-h-screen bg-[#0D1117]">
      <Hero />
      <Features />
      <Gallery />
      <Preview />
      <Testimonials />
      <Download />
    </div>
  )
}

export default App
