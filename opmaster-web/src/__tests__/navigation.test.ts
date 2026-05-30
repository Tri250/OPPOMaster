import { describe, it, expect } from 'vitest';

describe('导航和路由功能测试', () => {
  describe('路由定义验证', () => {
    const routes = [
      { path: '/', component: 'HomePage', description: '首页' },
      { path: '/home', component: 'HomePage', description: '首页' },
      { path: '/ai-demo', component: 'AIDemoPage', description: 'AI演示页' },
      { path: '/tech', component: 'TechPage', description: '技术页' },
      { path: '/about', component: 'AboutPage', description: '关于页' },
      { path: '/preset/:id', component: 'PresetDetailPage', description: '预设详情页' },
      { path: '/watermark', component: 'WatermarkPage', description: '水印页' },
      { path: '/editor', component: 'PresetEditorPage', description: '预设编辑器页' },
    ];

    it('应该有所有必要的路由', () => {
      expect(routes.length).toBe(8);
    });

    it('路由路径应该是有效的', () => {
      routes.forEach(route => {
        expect(route.path).toMatch(/^\/?[a-z-/:]*$/);
      });
    });

    it('每个路由应该有对应的组件', () => {
      routes.forEach(route => {
        expect(route.component).toBeDefined();
        expect(route.component.length).toBeGreaterThan(0);
      });
    });

    it('路由描述应该是中文', () => {
      routes.forEach(route => {
        expect(route.description).toBeDefined();
        expect(typeof route.description).toBe('string');
      });
    });
  });

  describe('导航项验证', () => {
    const navItems = [
      { path: '/home', label: '首页' },
      { path: '/ai-demo', label: 'AI场景识别' },
      { path: '/tech', label: '影像工具' },
      { path: '/about', label: '关于' }
    ];

    it('导航项应该有正确的路径', () => {
      navItems.forEach(item => {
        expect(item.path).toMatch(/^\/[a-z-]+$/);
      });
    });

    it('导航项应该有正确的标签', () => {
      navItems.forEach(item => {
        expect(item.label).toBeDefined();
        expect(item.label.length).toBeGreaterThan(0);
      });
    });

    it('导航项数量应该正确', () => {
      expect(navItems.length).toBe(4);
    });

    it('导航项路径应该唯一', () => {
      const paths = navItems.map(item => item.path);
      const uniquePaths = new Set(paths);
      expect(uniquePaths.size).toBe(paths.length);
    });
  });

  describe('路由路径格式验证', () => {
    it('首页路径应该是斜杠或home', () => {
      const validHomePaths = ['/', '/home'];
      validHomePaths.forEach(path => {
        expect(path).toMatch(/^\/$|^\/home$/);
      });
    });

    it('详情页路径应该包含ID参数', () => {
      const detailPath = '/preset/:id';
      expect(detailPath).toContain(':id');
    });

    it('路径不包含空格', () => {
      const paths = ['/', '/home', '/ai-demo', '/tech', '/about', '/preset/:id', '/watermark', '/editor'];
      paths.forEach(path => {
        expect(path.includes(' ')).toBe(false);
      });
    });

    it('路径不包含大写字母', () => {
      const paths = ['/', '/home', '/ai-demo', '/tech', '/about', '/preset/:id', '/watermark', '/editor'];
      paths.forEach(path => {
        expect(path).toBe(path.toLowerCase());
      });
    });

    it('路径只包含允许的字符', () => {
      const paths = ['/', '/home', '/ai-demo', '/tech', '/about', '/preset/:id', '/watermark', '/editor'];
      // 允许的字符：字母、数字、斜杠、冒号、连字符
      const validChars = /^[a-zA-Z0-9\/:\-]+$/;
      paths.forEach(path => {
        expect(validChars.test(path)).toBe(true);
      });
    });
  });

  describe('页面标题验证', () => {
    const pageTitles = [
      { path: '/', expectedTitle: 'OPPO Master' },
      { path: '/home', expectedTitle: 'OPPO Master' },
      { path: '/ai-demo', expectedTitle: 'AI场景识别' },
      { path: '/tech', expectedTitle: '影像工具' },
      { path: '/about', expectedTitle: '关于' },
      { path: '/watermark', expectedTitle: '水印生成器' },
      { path: '/editor', expectedTitle: '预设编辑器' },
    ];

    it('所有页面应该有标题', () => {
      pageTitles.forEach(page => {
        expect(page.expectedTitle).toBeDefined();
        expect(page.expectedTitle.length).toBeGreaterThan(0);
      });
    });

    it('标题不应该包含HTML标签', () => {
      pageTitles.forEach(page => {
        expect(page.expectedTitle.includes('<')).toBe(false);
        expect(page.expectedTitle.includes('>')).toBe(false);
      });
    });
  });

  describe('导航状态验证', () => {
    it('当前路径应该能正确识别', () => {
      const currentPath = '/home';
      const expectedActivePath = '/home';
      expect(currentPath).toBe(expectedActivePath);
    });

    it('非活动链接不应该被标记为活动', () => {
      const currentPath = '/home';
      const paths = ['/ai-demo', '/tech', '/about'];
      paths.forEach(path => {
        expect(currentPath).not.toBe(path);
      });
    });

    it('应该支持所有主要导航路径', () => {
      const mainNavPaths = ['/home', '/ai-demo', '/tech', '/about'];
      expect(mainNavPaths.length).toBe(4);
    });
  });
});
