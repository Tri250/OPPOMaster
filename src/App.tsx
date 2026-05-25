import { useState } from 'react';
import { ProjectTree } from './components/ProjectTree';
import { CodeViewer } from './components/CodeViewer';
import { RunPanel } from './components/RunPanel';
import { Code, Smartphone, Settings, Github, ChevronLeft, ChevronRight } from 'lucide-react';

function App() {
  const [selectedFile, setSelectedFile] = useState<string | null>(null);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [activeTab, setActiveTab] = useState<'code' | 'preview'>('code');

  const handleFileSelect = (filePath: string) => {
    setSelectedFile(filePath);
  };

  const handleCloseCodeViewer = () => {
    setSelectedFile(null);
  };

  return (
    <div className="h-screen flex flex-col bg-gray-900">
      <header className="h-12 bg-gray-800 border-b border-gray-700 flex items-center justify-between px-4">
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center">
              <Code className="w-5 h-5 text-white" />
            </div>
            <span className="text-lg font-semibold text-white">OMaster</span>
            <span className="text-xs px-2 py-0.5 bg-gray-700 rounded text-gray-400">v1.0.0</span>
          </div>
          <div className="h-6 w-px bg-gray-700" />
          <div className="flex items-center gap-1">
            <button
              onClick={() => setActiveTab('code')}
              className={`px-3 py-1.5 rounded text-sm font-medium transition-all ${
                activeTab === 'code'
                  ? 'bg-gray-700 text-white'
                  : 'text-gray-400 hover:text-white hover:bg-gray-700/50'
              }`}
            >
              <Code className="w-4 h-4 inline mr-1" />
              Code
            </button>
            <button
              onClick={() => setActiveTab('preview')}
              className={`px-3 py-1.5 rounded text-sm font-medium transition-all ${
                activeTab === 'preview'
                  ? 'bg-gray-700 text-white'
                  : 'text-gray-400 hover:text-white hover:bg-gray-700/50'
              }`}
            >
              <Smartphone className="w-4 h-4 inline mr-1" />
              Preview
            </button>
          </div>
        </div>
        
        <div className="flex items-center gap-2">
          <button className="p-2 hover:bg-gray-700 rounded transition-colors" title="Settings">
            <Settings className="w-5 h-5 text-gray-400" />
          </button>
          <button className="p-2 hover:bg-gray-700 rounded transition-colors" title="GitHub">
            <Github className="w-5 h-5 text-gray-400" />
          </button>
        </div>
      </header>

      <div className="flex-1 flex overflow-hidden">
        <div className={`transition-all duration-300 ${sidebarCollapsed ? 'w-12' : 'w-64'}`}>
          <ProjectTree 
            onFileSelect={handleFileSelect} 
            selectedFile={selectedFile} 
          />
        </div>
        
        <button
          onClick={() => setSidebarCollapsed(!sidebarCollapsed)}
          className="w-6 bg-gray-800 border-r border-gray-700 flex items-center justify-center hover:bg-gray-700/50 transition-colors"
        >
          {sidebarCollapsed ? (
            <ChevronRight className="w-4 h-4 text-gray-400" />
          ) : (
            <ChevronLeft className="w-4 h-4 text-gray-400" />
          )}
        </button>

        <main className="flex-1 flex flex-col overflow-hidden">
          {selectedFile ? (
            <CodeViewer filePath={selectedFile} onClose={handleCloseCodeViewer} />
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center bg-gray-900">
              <div className="text-center">
                <div className="w-20 h-20 rounded-full bg-gray-800 flex items-center justify-center mx-auto mb-6">
                  <Code className="w-10 h-10 text-gray-500" />
                </div>
                <h2 className="text-xl font-semibold text-gray-300 mb-2">
                  Welcome to OMaster Studio
                </h2>
                <p className="text-gray-500 mb-8">
                  Select a file from the project tree to view its contents
                </p>
                <div className="grid grid-cols-3 gap-4 max-w-md mx-auto">
                  <div className="p-4 bg-gray-800 rounded-lg">
                    <div className="text-2xl font-bold text-blue-500">24</div>
                    <div className="text-sm text-gray-400">Kotlin Files</div>
                  </div>
                  <div className="p-4 bg-gray-800 rounded-lg">
                    <div className="text-2xl font-bold text-green-500">6</div>
                    <div className="text-sm text-gray-400">Presets</div>
                  </div>
                  <div className="p-4 bg-gray-800 rounded-lg">
                    <div className="text-2xl font-bold text-purple-500">3</div>
                    <div className="text-sm text-gray-400">Features</div>
                  </div>
                </div>
              </div>
            </div>
          )}
        </main>

        <div className="w-80 border-l border-gray-700">
          <RunPanel />
        </div>
      </div>
    </div>
  );
}

export default App;
