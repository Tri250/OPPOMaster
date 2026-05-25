import React, { useState } from 'react';
import { ChevronRight, ChevronDown, Folder, File, FileCode, FileJson, FileText } from 'lucide-react';

interface TreeNode {
  name: string;
  type: 'folder' | 'file';
  extension?: string;
  children?: TreeNode[];
}

const projectStructure: TreeNode = {
  name: 'OMaster',
  type: 'folder',
  children: [
    {
      name: 'app',
      type: 'folder',
      children: [
        {
          name: 'src',
          type: 'folder',
          children: [
            {
              name: 'main',
              type: 'folder',
              children: [
                {
                  name: 'java',
                  type: 'folder',
                  children: [
                    {
                      name: 'com',
                      type: 'folder',
                      children: [
                        {
                          name: 'omaster',
                          type: 'folder',
                          children: [
                            {
                              name: 'app',
                              type: 'folder',
                              children: [
                                { name: 'data', type: 'folder', children: [
                                  { name: 'PreferencesDataStore.kt', type: 'file', extension: 'kt' },
                                  { name: 'PresetRepository.kt', type: 'file', extension: 'kt' },
                                ]},
                                { name: 'di', type: 'folder', children: [
                                  { name: 'NetworkModule.kt', type: 'file', extension: 'kt' },
                                ]},
                                { name: 'model', type: 'folder', children: [
                                  { name: 'AiAdjustmentParams.kt', type: 'file', extension: 'kt' },
                                  { name: 'CameraParams.kt', type: 'file', extension: 'kt' },
                                  { name: 'Preset.kt', type: 'file', extension: 'kt' },
                                  { name: 'SceneType.kt', type: 'file', extension: 'kt' },
                                ]},
                                { name: 'navigation', type: 'folder', children: [
                                  { name: 'AppNavHost.kt', type: 'file', extension: 'kt' },
                                  { name: 'Screen.kt', type: 'file', extension: 'kt' },
                                ]},
                                { name: 'network', type: 'folder', children: [
                                  { name: 'PresetApi.kt', type: 'file', extension: 'kt' },
                                ]},
                                { name: 'service', type: 'folder', children: [
                                  { name: 'AiService.kt', type: 'file', extension: 'kt' },
                                  { name: 'FluidCloudService.kt', type: 'file', extension: 'kt' },
                                ]},
                                { name: 'ui', type: 'folder', children: [
                                  { name: 'components', type: 'folder', children: [
                                    { name: 'FilterChips.kt', type: 'file', extension: 'kt' },
                                    { name: 'PresetCard.kt', type: 'file', extension: 'kt' },
                                    { name: 'SearchBar.kt', type: 'file', extension: 'kt' },
                                    { name: 'ErrorSnackbar.kt', type: 'file', extension: 'kt' },
                                  ]},
                                  { name: 'screens', type: 'folder', children: [
                                    { name: 'AiFineTuneScreen.kt', type: 'file', extension: 'kt' },
                                    { name: 'DetailScreen.kt', type: 'file', extension: 'kt' },
                                    { name: 'HomeScreen.kt', type: 'file', extension: 'kt' },
                                    { name: 'SceneDetectionScreen.kt', type: 'file', extension: 'kt' },
                                    { name: 'SettingsScreen.kt', type: 'file', extension: 'kt' },
                                  ]},
                                  { name: 'theme', type: 'folder', children: [
                                    { name: 'Color.kt', type: 'file', extension: 'kt' },
                                    { name: 'Theme.kt', type: 'file', extension: 'kt' },
                                    { name: 'Type.kt', type: 'file', extension: 'kt' },
                                  ]},
                                ]},
                                { name: 'viewmodel', type: 'folder', children: [
                                  { name: 'MainViewModel.kt', type: 'file', extension: 'kt' },
                                ]},
                                { name: 'MainActivity.kt', type: 'file', extension: 'kt' },
                                { name: 'OMasterApplication.kt', type: 'file', extension: 'kt' },
                              ],
                            },
                          ],
                        },
                      ],
                    },
                  ],
                },
                {
                  name: 'res',
                  type: 'folder',
                  children: [
                    { name: 'mipmap-anydpi-v26', type: 'folder', children: [
                      { name: 'ic_launcher.xml', type: 'file', extension: 'xml' },
                      { name: 'ic_launcher_round.xml', type: 'file', extension: 'xml' },
                    ]},
                    { name: 'values', type: 'folder', children: [
                      { name: 'colors.xml', type: 'file', extension: 'xml' },
                      { name: 'strings.xml', type: 'file', extension: 'xml' },
                      { name: 'themes.xml', type: 'file', extension: 'xml' },
                    ]},
                  ],
                },
                { name: 'AndroidManifest.xml', type: 'file', extension: 'xml' },
              ],
            },
            {
              name: 'test',
              type: 'folder',
              children: [
                { name: 'FilterTypeTest.kt', type: 'file', extension: 'kt' },
                { name: 'PresetTest.kt', type: 'file', extension: 'kt' },
              ],
            },
          ],
        },
        { name: 'build.gradle.kts', type: 'file', extension: 'kts' },
        { name: 'proguard-rules.pro', type: 'file', extension: 'pro' },
      ],
    },
    { name: 'gradle', type: 'folder', children: [
      { name: 'wrapper', type: 'folder', children: [
        { name: 'gradle-wrapper.properties', type: 'file', extension: 'properties' },
      ]},
    ]},
    { name: 'build.gradle.kts', type: 'file', extension: 'kts' },
    { name: 'settings.gradle.kts', type: 'file', extension: 'kts' },
    { name: 'gradle.properties', type: 'file', extension: 'properties' },
    { name: 'README.md', type: 'file', extension: 'md' },
    { name: 'BUILD_GUIDE.md', type: 'file', extension: 'md' },
  ],
};

interface ProjectTreeProps {
  onFileSelect: (filePath: string) => void;
  selectedFile: string | null;
}

function TreeItem({
  node,
  depth = 0,
  onFileSelect,
  selectedFile,
  path = '',
}: {
  node: TreeNode;
  depth?: number;
  onFileSelect: (filePath: string) => void;
  selectedFile: string | null;
  path?: string;
}) {
  const [isExpanded, setIsExpanded] = useState(node.type === 'folder');
  const currentPath = path ? `${path}/${node.name}` : node.name;
  const isSelected = selectedFile === currentPath;

  if (node.type === 'folder') {
    return (
      <div>
        <div
          className={`flex items-center gap-1 px-2 py-1 cursor-pointer hover:bg-gray-700/50 rounded transition-colors ${
            isSelected ? 'bg-gray-700/70' : ''
          }`}
          style={{ paddingLeft: `${depth * 12 + 8}px` }}
          onClick={() => setIsExpanded(!isExpanded)}
        >
          {node.children && node.children.length > 0 ? (
            isExpanded ? (
              <ChevronDown className="w-4 h-4 text-gray-400" />
            ) : (
              <ChevronRight className="w-4 h-4 text-gray-400" />
            )
          ) : (
            <span className="w-4" />
          )}
          <Folder className="w-4 h-4 text-yellow-500" />
          <span className="text-sm text-gray-300">{node.name}</span>
        </div>
        {isExpanded && node.children && (
          <div>
            {node.children.map((child, index) => (
              <TreeItem
                key={`${currentPath}-${index}`}
                node={child}
                depth={depth + 1}
                onFileSelect={onFileSelect}
                selectedFile={selectedFile}
                path={currentPath}
              />
            ))}
          </div>
        )}
      </div>
    );
  }

  return (
    <div
      className={`flex items-center gap-1 px-2 py-1 cursor-pointer hover:bg-gray-700/50 rounded transition-colors ${
        isSelected ? 'bg-gray-700/70' : ''
      }`}
      style={{ paddingLeft: `${depth * 12 + 8}px` }}
      onClick={() => onFileSelect(currentPath)}
    >
      <span className="w-4" />
      {node.extension === 'kt' && <FileCode className="w-4 h-4 text-purple-400" />}
      {node.extension === 'xml' && <FileCode className="w-4 h-4 text-orange-400" />}
      {node.extension === 'json' && <FileJson className="w-4 h-4 text-yellow-400" />}
      {node.extension === 'md' && <FileText className="w-4 h-4 text-blue-400" />}
      {!['kt', 'xml', 'json', 'md'].includes(node.extension || '') && (
        <File className="w-4 h-4 text-gray-400" />
      )}
      <span className="text-sm text-gray-300">{node.name}</span>
    </div>
  );
}

export function ProjectTree({ onFileSelect, selectedFile }: ProjectTreeProps) {
  return (
    <div className="h-full bg-gray-800 border-r border-gray-700">
      <div className="px-4 py-3 border-b border-gray-700">
        <h2 className="text-sm font-medium text-gray-300">Project</h2>
      </div>
      <div className="overflow-y-auto h-[calc(100%-48px)]">
        <TreeItem
          node={projectStructure}
          onFileSelect={onFileSelect}
          selectedFile={selectedFile}
        />
      </div>
    </div>
  );
}
