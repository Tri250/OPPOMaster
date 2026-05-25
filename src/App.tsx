import { useState } from 'react';
import { ProjectTree } from './components/ProjectTree';
import { CodeViewer } from './components/CodeViewer';
import { RunPanel } from './components/RunPanel';
import { AppPreview } from './components/AppPreview';
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
        {activeTab === 'code' ? (
          <>
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
          </>
        ) : (
          <div className="flex-1 flex items-center justify-center bg-gray-950 p-8">
            {/* Phone Frame */}
            <div className="relative">
              {/* Phone Bezel */}
              <div className="w-[360px] h-[720px] bg-gray-800 rounded-[3rem] p-3 shadow-2xl">
                {/* Phone Screen */}
                <div className="w-full h-full bg-gray-900 rounded-[2.5rem] overflow-hidden relative">
                  {/* Dynamic Island */}
                  <div className="absolute top-3 left-1/2 -translate-x-1/2 w-24 h-7 bg-black rounded-full z-50" />
                  
                  {/* Status Bar */}
                  <div className="absolute top-0 left-0 right-0 h-12 flex items-center justify-between px-6 z-40">
                        <span className="text-white text-sm font-medium">9:41</span>
                        <div className="flex items-center gap-1">
                          <div className="w-4 h-4 rounded-sm bg-white/80" />
                          <div className="w-4 h-4 rounded-sm bg-white/80" />
                          <div className="w-6 h-3 rounded-sm border border-white/80 relative">
                            <div className="absolute inset-0.5 bg-white/80 rounded-sm" />
                          </div>
                        </div>
                      </div>

                  {/* App Content */}
                  <div className="w-full h-full pt-12">
                    <AppPreview />
                  </div>
                </div>
              </div>
              
              {/* Phone Reflection */}
              <div className="absolute inset-0 rounded-[3rem] bg-gradient-to-br from-white/5 to-transparent pointer-events-none" />
            </div>
            
            {/* Info Panel */}
            <div className="ml-8 w-64">
              <h3 className="text-white font-semibold mb-4">OMaster App Preview</h3>
              <div className="space-y-3 text-sm text-gray-400">
                <div className="flex items-center gap-2">
                  <div className="w-2 h-2 rounded-full bg-green-500" />
                  <span>Home Screen - 预设列表</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-2 h-2 rounded-full bg-blue-500" />
                  <span>Detail Screen - 预设详情</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="w-2 h-2 rounded-full bg-purple-500" />
                  <span>Settings - 应用设置</span>
                </div>
              </div>
              <div className="mt-6 p-4 bg-gray-800 rounded-lg">
                <p className="text-xs text-gray-500">
                  点击手机界面中的预设卡片查看详情，点击右上角设置图标进入设置页面。
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;
