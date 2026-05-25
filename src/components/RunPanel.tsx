import React, { useState, useEffect } from 'react';
import { 
  Play, 
  Square, 
  RotateCcw, 
  Bug, 
  Terminal, 
  Smartphone, 
  CheckCircle, 
  AlertCircle,
  Loader2,
  ChevronRight,
  ChevronDown
} from 'lucide-react';

interface LogEntry {
  id: number;
  type: 'info' | 'warning' | 'error' | 'success';
  message: string;
  timestamp: string;
}

const mockLogs: LogEntry[] = [
  { id: 1, type: 'info', message: 'OMaster Application started', timestamp: '10:30:00' },
  { id: 2, type: 'info', message: 'Loading presets from repository...', timestamp: '10:30:01' },
  { id: 3, type: 'success', message: 'Loaded 6 presets successfully', timestamp: '10:30:02' },
  { id: 4, type: 'info', message: 'Initializing FluidCloudService...', timestamp: '10:30:03' },
  { id: 5, type: 'success', message: 'FluidCloudService started', timestamp: '10:30:04' },
  { id: 6, type: 'info', message: 'Applying theme mode: SYSTEM', timestamp: '10:30:05' },
  { id: 7, type: 'info', message: 'HomeScreen rendered', timestamp: '10:30:06' },
];

export function RunPanel() {
  const [isRunning, setIsRunning] = useState(false);
  const [isDebugging, setIsDebugging] = useState(false);
  const [logs, setLogs] = useState<LogEntry[]>(mockLogs);
  const [expandedSections, setExpandedSections] = useState({
    logs: true,
    devices: true,
    build: true,
  });
  const [buildOutput, setBuildOutput] = useState('');
  const [buildStatus, setBuildStatus] = useState<'idle' | 'building' | 'success' | 'error'>('idle');

  useEffect(() => {
    if (isRunning) {
      const interval = setInterval(() => {
        const newLog: LogEntry = {
          id: Date.now(),
          type: ['info', 'success', 'info', 'info'][Math.floor(Math.random() * 4)] as LogEntry['type'],
          message: [
            'User interaction detected',
            'Preset data synchronized',
            'Theme mode updated',
            'Memory usage: 45MB',
            'Background task completed',
            'Network ping successful',
          ][Math.floor(Math.random() * 6)],
          timestamp: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
        };
        setLogs(prev => [...prev.slice(-49), newLog]);
      }, 3000);
      return () => clearInterval(interval);
    }
  }, [isRunning]);

  const handleBuild = () => {
    setBuildStatus('building');
    setBuildOutput('');
    
    const buildSteps = [
      '> ./gradlew assembleDebug',
      '',
      'Starting Gradle Daemon...',
      'Gradle Daemon started in 2.3s',
      '',
      '> Task :app:preBuild',
      '> Task :app:compileDebugKotlin',
      '> Task :app:compileDebugJavaWithJavac',
      '> Task :app:mergeDebugResources',
      '> Task :app:processDebugManifest',
      '> Task :app:compileDebugSources',
      '> Task :app:linkDebug',
      '> Task :app:stripDebugDebugSymbols',
      '> Task :app:packageDebug',
      '',
      'BUILD SUCCESSFUL in 45s',
      '',
      'APK location: app/build/outputs/apk/debug/app-debug.apk',
    ];

    let stepIndex = 0;
    const interval = setInterval(() => {
      if (stepIndex < buildSteps.length) {
        setBuildOutput(prev => prev + buildSteps[stepIndex] + '\n');
        stepIndex++;
      } else {
        clearInterval(interval);
        setBuildStatus('success');
      }
    }, 200);
  };

  const handleRun = () => {
    if (isRunning) {
      setIsRunning(false);
      setLogs(prev => [...prev, {
        id: Date.now(),
        type: 'info',
        message: 'Application stopped',
        timestamp: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
      }]);
    } else {
      setIsRunning(true);
      setLogs(prev => [...prev, {
        id: Date.now(),
        type: 'success',
        message: 'Application started successfully',
        timestamp: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
      }]);
    }
  };

  const handleDebug = () => {
    setIsDebugging(!isDebugging);
    if (isDebugging) {
      setLogs(prev => [...prev, {
        id: Date.now(),
        type: 'info',
        message: 'Debug mode disabled',
        timestamp: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
      }]);
    } else {
      setLogs(prev => [...prev, {
        id: Date.now(),
        type: 'info',
        message: 'Debug mode enabled. Waiting for debugger...',
        timestamp: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
      }, {
        id: Date.now() + 1,
        type: 'success',
        message: 'Debugger attached',
        timestamp: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
      }]);
    }
  };

  const handleReset = () => {
    setLogs([]);
    setIsRunning(false);
    setIsDebugging(false);
    setBuildOutput('');
    setBuildStatus('idle');
  };

  const toggleSection = (section: 'logs' | 'devices' | 'build') => {
    setExpandedSections(prev => ({ ...prev, [section]: !prev[section] }));
  };

  return (
    <div className="h-full flex flex-col bg-gray-900">
      <div className="flex items-center justify-between px-4 py-3 bg-gray-800 border-b border-gray-700">
        <div className="flex items-center gap-2">
          <Terminal className="w-4 h-4 text-green-500" />
          <span className="text-sm font-medium text-gray-300">Run & Debug</span>
        </div>
        <div className="flex items-center gap-1">
          <button
            onClick={handleRun}
            className={`flex items-center gap-2 px-3 py-1.5 rounded text-sm font-medium transition-all ${
              isRunning
                ? 'bg-red-600 hover:bg-red-700 text-white'
                : 'bg-green-600 hover:bg-green-700 text-white'
            }`}
          >
            {isRunning ? <Square className="w-4 h-4" /> : <Play className="w-4 h-4" />}
            {isRunning ? 'Stop' : 'Run'}
          </button>
          <button
            onClick={handleDebug}
            className={`flex items-center gap-2 px-3 py-1.5 rounded text-sm font-medium transition-all ${
              isDebugging
                ? 'bg-purple-600 hover:bg-purple-700 text-white'
                : 'bg-gray-700 hover:bg-gray-600 text-gray-300'
            }`}
          >
            <Bug className="w-4 h-4" />
            Debug
          </button>
          <button
            onClick={handleReset}
            className="flex items-center gap-2 px-3 py-1.5 rounded text-sm font-medium bg-gray-700 hover:bg-gray-600 text-gray-300 transition-all"
          >
            <RotateCcw className="w-4 h-4" />
            Reset
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-auto">
        <div className="border-b border-gray-700">
          <div
            onClick={() => toggleSection('build')}
            className="w-full flex items-center justify-between px-4 py-2 hover:bg-gray-800/50 transition-colors cursor-pointer"
          >
            <div className="flex items-center gap-2">
              {expandedSections.build ? (
                <ChevronDown className="w-4 h-4 text-gray-400" />
              ) : (
                <ChevronRight className="w-4 h-4 text-gray-400" />
              )}
              <span className="text-sm text-gray-300">Build Output</span>
              {buildStatus === 'building' && (
                <Loader2 className="w-4 h-4 text-yellow-500 animate-spin" />
              )}
              {buildStatus === 'success' && (
                <CheckCircle className="w-4 h-4 text-green-500" />
              )}
              {buildStatus === 'error' && (
                <AlertCircle className="w-4 h-4 text-red-500" />
              )}
            </div>
            <button
              onClick={(e) => {
                e.stopPropagation();
                handleBuild();
              }}
              disabled={buildStatus === 'building'}
              className="px-3 py-1 rounded text-xs font-medium bg-blue-600 hover:bg-blue-700 text-white transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Build
            </button>
          </div>
          {expandedSections.build && (
            <div className="px-4 py-2 bg-gray-800/50">
              <pre className="text-xs text-gray-400 font-mono whitespace-pre-wrap max-h-32 overflow-auto">
                {buildOutput || 'Click "Build" to compile the project...'}
              </pre>
            </div>
          )}
        </div>

        <div className="border-b border-gray-700">
          <button
            onClick={() => toggleSection('devices')}
            className="w-full flex items-center justify-between px-4 py-2 hover:bg-gray-800/50 transition-colors"
          >
            <div className="flex items-center gap-2">
              {expandedSections.devices ? (
                <ChevronDown className="w-4 h-4 text-gray-400" />
              ) : (
                <ChevronRight className="w-4 h-4 text-gray-400" />
              )}
              <Smartphone className="w-4 h-4 text-blue-400" />
              <span className="text-sm text-gray-300">Devices</span>
            </div>
          </button>
          {expandedSections.devices && (
            <div className="px-4 py-2 bg-gray-800/50">
              <div className="space-y-2">
                <div className="flex items-center gap-3 p-2 bg-gray-700/50 rounded">
                  <div className="w-8 h-8 rounded bg-blue-600 flex items-center justify-center">
                    <Smartphone className="w-4 h-4 text-white" />
                  </div>
                  <div className="flex-1">
                    <div className="text-sm text-gray-300">Pixel 8 Pro API 35</div>
                    <div className="text-xs text-gray-500">Android 16</div>
                  </div>
                  <div className="flex items-center gap-1">
                    <div className="w-2 h-2 rounded-full bg-green-500" />
                    <span className="text-xs text-green-500">Online</span>
                  </div>
                </div>
                <div className="flex items-center gap-3 p-2 bg-gray-700/50 rounded">
                  <div className="w-8 h-8 rounded bg-orange-600 flex items-center justify-center">
                    <Smartphone className="w-4 h-4 text-white" />
                  </div>
                  <div className="flex-1">
                    <div className="text-sm text-gray-300">OPPO Find X8 Pro</div>
                    <div className="text-xs text-gray-500">Android 16 (ColorOS 16)</div>
                  </div>
                  <div className="flex items-center gap-1">
                    <div className="w-2 h-2 rounded-full bg-green-500" />
                    <span className="text-xs text-green-500">Online</span>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>

        <div>
          <button
            onClick={() => toggleSection('logs')}
            className="w-full flex items-center justify-between px-4 py-2 hover:bg-gray-800/50 transition-colors"
          >
            <div className="flex items-center gap-2">
              {expandedSections.logs ? (
                <ChevronDown className="w-4 h-4 text-gray-400" />
              ) : (
                <ChevronRight className="w-4 h-4 text-gray-400" />
              )}
              <Terminal className="w-4 h-4 text-gray-400" />
              <span className="text-sm text-gray-300">Logs</span>
              <span className="text-xs text-gray-500">({logs.length})</span>
            </div>
          </button>
          {expandedSections.logs && (
            <div className="px-4 py-2 bg-gray-800/30 max-h-64 overflow-auto">
              {logs.map((log) => (
                <div
                  key={log.id}
                  className="flex items-start gap-2 py-1 text-xs font-mono"
                >
                  <span className="text-gray-500 shrink-0">{log.timestamp}</span>
                  {log.type === 'success' && (
                    <CheckCircle className="w-3 h-3 text-green-500 shrink-0 mt-0.5" />
                  )}
                  {log.type === 'warning' && (
                    <AlertCircle className="w-3 h-3 text-yellow-500 shrink-0 mt-0.5" />
                  )}
                  {log.type === 'error' && (
                    <AlertCircle className="w-3 h-3 text-red-500 shrink-0 mt-0.5" />
                  )}
                  {log.type === 'info' && (
                    <span className="w-3 h-3 text-gray-500 shrink-0 mt-0.5" />
                  )}
                  <span className={`${
                    log.type === 'success' ? 'text-green-400' :
                    log.type === 'warning' ? 'text-yellow-400' :
                    log.type === 'error' ? 'text-red-400' : 'text-gray-400'
                  }`}>
                    {log.message}
                  </span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="px-4 py-2 bg-gray-800 border-t border-gray-700">
        <div className="flex items-center justify-between text-xs text-gray-500">
          <span>
            Status: {isRunning ? 'Running' : 'Idle'} {isDebugging && '| Debugging'}
          </span>
          <span>
            Logs: {logs.length} entries
          </span>
        </div>
      </div>
    </div>
  );
}
