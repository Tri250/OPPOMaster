
## 1. Architecture Design
纯前端架构，使用React构建单页应用架构

```mermaid
graph TD
    Frontend[React Frontend] --> Routing[React Router]
    Frontend --> State[Zustand Store]
    Frontend --> UI[React Components]
    UI --> Home[Home Page]
    UI --> Detail[Detail Page]
    UI --> Settings[Settings Page]
    State --> Data[Mock Data]
```

## 2. Technology Description
- 前端: React@18 + TypeScript + tailwindcss@3 + vite
- 初始化工具: vite-init
- 后端: None（纯前端应用）
- 状态管理: zustand
- 路由: react-router-dom

## 3. Route Definitions
| Route | Purpose |
|-------|---------|
| / | 首页 - 预设列表和搜索筛选 |
| /detail/:id | 详情页 - 显示预设详细信息 |
| /settings | 设置页 - 主题切换和应用设置 |

## 4. Data Model

### 4.1 Data Model Definition
```mermaid
erDiagram
    Preset {
        string id
        string name
        string coverPath
        string deviceModel
        string source
        boolean isFavorite
        CameraParams cameraParams
    }
    CameraParams {
        string mode
        string filter
        int iso
        string shutter
        string ev
        string wb
        boolean hasselblad_hncs
    }
```

### 4.2 TypeScript Type Definitions
```typescript
export interface CameraParams {
  mode: string;
  filter: string;
  iso: number;
  shutter: string;
  ev: string;
  wb: string;
  hasselblad_hncs: boolean;
}

export interface Preset {
  id: string;
  name: string;
  coverPath: string;
  deviceModel: string;
  source: string;
  isFavorite: boolean;
  cameraParams?: CameraParams;
}

export type FilterType = 'ALL' | 'FAVORITES' | 'HNCS' | 'FIND_X' | 'RENO';

export type ThemeMode = 'light' | 'dark' | 'system';
```

## 5. File Structure
```
omaster-web/
├── src/
│   ├── components/
│   │   ├── FilterChips.tsx
│   │   ├── PresetCard.tsx
│   │   └── SearchBar.tsx
│   ├── pages/
│   │   ├── Home.tsx
│   │   ├── Detail.tsx
│   │   └── Settings.tsx
│   ├── store/
│   │   └── useStore.ts
│   ├── types/
│   │   └── index.ts
│   ├── App.tsx
│   ├── main.tsx
│   └── index.css
├── package.json
├── vite.config.ts
├── tailwind.config.js
└── tsconfig.json
```
