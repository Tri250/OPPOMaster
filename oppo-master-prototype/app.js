/* ============================================================
   OPPO Master · Hasselblad Professional Imaging
   App Renderer · ColorOS 16 金标规范
   ============================================================ */

(function () {
  'use strict';

  const grid = document.getElementById('phoneGrid');
  if (!grid) return;

  /**
   * Build a single phone stage with the screen content
   */
  function buildPhoneStage(screen) {
    const renderer = SCREEN_RENDERERS[screen.type];
    if (!renderer) return '';

    return `
      <div class="phone-stage" data-screen="${screen.type}">
        <div class="phone-label">
          <div class="phone-label-left">
            <span class="phone-num">${screen.num}</span>
            <span class="phone-name">${screen.name}</span>
          </div>
          <span class="phone-tag">${screen.tag}</span>
        </div>
        <div class="phone-frame">
          <div class="phone-screen">
            ${dynamicIsland()}
            ${renderer()}
          </div>
        </div>
      </div>
    `;
  }

  /**
   * Render all phone screens into the grid
   */
  function renderAll() {
    const html = SCREENS.map(buildPhoneStage).join('');
    grid.innerHTML = html;

    // Trigger entrance animation with staggered delay
    const stages = grid.querySelectorAll('.phone-stage');
    stages.forEach((stage, idx) => {
      stage.style.opacity = '0';
      stage.style.transform = 'translateY(40px)';
      stage.style.transition =
        'opacity 0.8s cubic-bezier(0.22, 1, 0.36, 1), transform 0.8s cubic-bezier(0.22, 1, 0.36, 1)';
      stage.style.transitionDelay = `${idx * 0.08}s`;

      // Use requestAnimationFrame to ensure paint before applying transition
      requestAnimationFrame(() => {
        requestAnimationFrame(() => {
          stage.style.opacity = '1';
          stage.style.transform = 'translateY(0)';
        });
      });
    });
  }

  /**
   * Wire up category chips for filtering
   */
  function bindFilters() {
    const chips = document.querySelectorAll('.section-nav .nav-chip');
    chips.forEach((chip) => {
      chip.addEventListener('click', () => {
        chips.forEach((c) => c.classList.remove('active'));
        chip.classList.add('active');
        // Animation feedback
        chip.style.transform = 'scale(0.95)';
        setTimeout(() => {
          chip.style.transform = '';
        }, 150);
      });
    });
  }

  /**
   * Add subtle parallax on phone hover
   */
  function bindParallax() {
    const frames = document.querySelectorAll('.phone-frame');
    frames.forEach((frame) => {
      frame.addEventListener('mousemove', (e) => {
        const rect = frame.getBoundingClientRect();
        const x = (e.clientX - rect.left) / rect.width - 0.5;
        const y = (e.clientY - rect.top) / rect.height - 0.5;
        frame.style.transform =
          `translateY(-8px) scale(1.01) rotateX(${-y * 3}deg) rotateY(${x * 3}deg)`;
      });

      frame.addEventListener('mouseleave', () => {
        frame.style.transform = '';
      });
    });
  }

  /**
   * Toggle interaction handlers for chips, tabs, cards
   */
  function bindInteractions() {
    // Category tabs in home screen
    document.querySelectorAll('.cat-tab').forEach((tab) => {
      tab.addEventListener('click', () => {
        const tabs = tab.parentElement.querySelectorAll('.cat-tab');
        tabs.forEach((t) => t.classList.remove('active'));
        tab.classList.add('active');
      });
    });

    // Style chips in create screen
    document.querySelectorAll('.style-chip').forEach((chip) => {
      chip.addEventListener('click', () => {
        const chips = chip.parentElement.querySelectorAll('.style-chip');
        chips.forEach((c) => c.classList.remove('active'));
        chip.classList.add('active');
      });
    });

    // Watermark templates
    document.querySelectorAll('.wm-template').forEach((tpl) => {
      tpl.addEventListener('click', () => {
        const tpls = tpl.parentElement.querySelectorAll('.wm-template');
        tpls.forEach((t) => t.classList.remove('active'));
        tpl.classList.add('active');
      });
    });

    // Watermark tools
    document.querySelectorAll('.wm-tool').forEach((tool) => {
      tool.addEventListener('click', () => {
        const tools = tool.parentElement.querySelectorAll('.wm-tool');
        tools.forEach((t) => t.classList.remove('active'));
        tool.classList.add('active');
      });
    });

    // Profile tabs
    document.querySelectorAll('.ptab').forEach((tab) => {
      tab.addEventListener('click', () => {
        const tabs = tab.parentElement.querySelectorAll('.ptab');
        tabs.forEach((t) => t.classList.remove('active'));
        tab.classList.add('active');
      });
    });

    // Bottom nav
    document.querySelectorAll('.nav-item').forEach((item) => {
      item.addEventListener('click', () => {
        const nav = item.parentElement.querySelectorAll('.nav-item');
        nav.forEach((n) => n.classList.remove('active'));
        item.classList.add('active');
      });
    });

    // Settings toggles
    document.querySelectorAll('.toggle').forEach((toggle) => {
      toggle.addEventListener('click', () => {
        toggle.classList.toggle('on');
      });
    });

    // Feedback categories
    document.querySelectorAll('.fb-cat').forEach((cat) => {
      cat.addEventListener('click', () => {
        const cats = cat.parentElement.querySelectorAll('.fb-cat');
        cats.forEach((c) => c.classList.remove('active'));
        cat.classList.add('active');
      });
    });

    // Preset cards (favorite toggle)
    document.querySelectorAll('.preset-fav').forEach((fav) => {
      fav.addEventListener('click', (e) => {
        e.stopPropagation();
        fav.style.transform = 'scale(1.3)';
        setTimeout(() => {
          fav.style.transform = '';
        }, 300);
      });
    });

    // Preset pills in camera
    document.querySelectorAll('.preset-pill').forEach((pill) => {
      pill.addEventListener('click', () => {
        const pills = pill.parentElement.querySelectorAll('.preset-pill');
        pills.forEach((p) => p.classList.remove('active'));
        pill.classList.add('active');
      });
    });
  }

  /**
   * Animate stats counter on scroll into view
   */
  function animateStats() {
    const stats = document.querySelectorAll('.stat-num');
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.style.opacity = '0';
            entry.target.style.transform = 'translateY(20px)';
            requestAnimationFrame(() => {
              entry.target.style.transition =
                'all 0.8s cubic-bezier(0.22, 1, 0.36, 1)';
              entry.target.style.opacity = '1';
              entry.target.style.transform = 'translateY(0)';
            });
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.3 }
    );

    stats.forEach((stat) => observer.observe(stat));
  }

  // ============================================================
  // Bootstrap
  // ============================================================
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

  function init() {
    renderAll();
    bindFilters();
    bindParallax();
    bindInteractions();
    animateStats();
  }
})();
