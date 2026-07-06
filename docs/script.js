document.addEventListener('DOMContentLoaded', () => {
  // Navbar scroll effect
  const navbar = document.querySelector('.navbar');
  
  window.addEventListener('scroll', () => {
    if (window.scrollY > 20) {
      navbar.style.background = 'rgba(11, 15, 25, 0.92)';
      navbar.style.boxShadow = '0 10px 30px rgba(0, 0, 0, 0.5)';
      navbar.style.borderColor = 'rgba(45, 212, 191, 0.3)';
    } else {
      navbar.style.background = 'rgba(11, 15, 25, 0.85)';
      navbar.style.boxShadow = 'none';
      navbar.style.borderColor = 'rgba(255, 255, 255, 0.08)';
    }
  }, { passive: true });

  // Mobile Drawer Menu
  const mobileBtn = document.querySelector('.mobile-menu-btn');
  const mobileDrawer = document.querySelector('.mobile-drawer');

  if (mobileBtn && mobileDrawer) {
    const toggleDrawer = () => {
      const isActive = mobileDrawer.classList.contains('active');
      if (isActive) {
        mobileDrawer.classList.remove('active');
        mobileBtn.innerHTML = '☰';
        document.body.style.overflow = '';
      } else {
        mobileDrawer.classList.add('active');
        mobileBtn.innerHTML = '✕';
        document.body.style.overflow = 'hidden';
      }
    };

    mobileBtn.addEventListener('click', (e) => {
      e.stopPropagation();
      toggleDrawer();
    });

    // Close drawer when clicking a link
    mobileDrawer.querySelectorAll('a').forEach(link => {
      link.addEventListener('click', () => {
        if (mobileDrawer.classList.contains('active')) {
          toggleDrawer();
        }
      });
    });

    // Close drawer when clicking outside
    document.addEventListener('click', (e) => {
      if (mobileDrawer.classList.contains('active') && !mobileDrawer.contains(e.target) && e.target !== mobileBtn) {
        toggleDrawer();
      }
    });
  }

  // GPU-Accelerated Scroll Animations with Intersection Observer
  const observerOptions = {
    root: null,
    rootMargin: '0px 0px -30px 0px',
    threshold: 0.08
  };

  const revealElements = document.querySelectorAll('.feature-card, .screenshot-item, .faq-item, .section-header');
  
  const scrollObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible');
        observer.unobserve(entry.target);
      }
    });
  }, observerOptions);

  revealElements.forEach((el, index) => {
    el.classList.add('reveal');
    const delay = (index % 3) * 0.1;
    el.style.transitionDelay = `${delay}s`;
    scrollObserver.observe(el);
  });

  // Lightbox Modal for Screenshots
  const lightbox = document.createElement('div');
  lightbox.className = 'lightbox';
  lightbox.innerHTML = `
    <div class="lightbox-content">
      <button class="lightbox-close" aria-label="Close modal">&times;</button>
      <img src="" alt="Screenshot preview">
    </div>
  `;
  document.body.appendChild(lightbox);

  const lightboxImg = lightbox.querySelector('img');
  const lightboxClose = lightbox.querySelector('.lightbox-close');

  const openLightbox = (src, alt) => {
    lightboxImg.src = src;
    lightboxImg.alt = alt;
    lightbox.classList.add('active');
    document.body.style.overflow = 'hidden';
  };

  const closeLightbox = () => {
    lightbox.classList.remove('active');
    document.body.style.overflow = '';
  };

  document.querySelectorAll('.screenshot-item').forEach(item => {
    item.addEventListener('click', () => {
      const img = item.querySelector('img');
      if (img) {
        openLightbox(img.src, img.alt);
      }
    });
  });

  lightboxClose.addEventListener('click', closeLightbox);
  lightbox.addEventListener('click', (e) => {
    if (e.target === lightbox) {
      closeLightbox();
    }
  });

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && lightbox.classList.contains('active')) {
      closeLightbox();
    }
  });

  // FAQ Accordion
  const faqItems = document.querySelectorAll('.faq-item');
  faqItems.forEach(item => {
    const question = item.querySelector('.faq-question');
    const answer = item.querySelector('.faq-answer');

    if (question && answer) {
      question.addEventListener('click', () => {
        const isActive = item.classList.contains('active');
        
        faqItems.forEach(otherItem => {
          otherItem.classList.remove('active');
          const otherAnswer = otherItem.querySelector('.faq-answer');
          if (otherAnswer) otherAnswer.style.maxHeight = null;
        });

        if (!isActive) {
          item.classList.add('active');
          answer.style.maxHeight = answer.scrollHeight + 30 + 'px';
        }
      });
    }
  });

  /* =========================================
     POWERFUL ONLINE JS LIBRARIES & INTERACTIVE ENGINE
     ========================================= */

  // 1. Initialize Vanilla-Tilt 3D Glare on Cards & Arena
  if (typeof VanillaTilt !== 'undefined') {
    VanillaTilt.init(document.querySelectorAll('.feature-card, .screenshot-item, .demo-arena'), {
      max: 6,
      speed: 400,
      glare: true,
      'max-glare': 0.15,
      scale: 1.02
    });
  }

  // 2. Initialize Typed.js Dynamic Typing in Hero
  if (typeof Typed !== 'undefined' && document.getElementById('typed-output')) {
    new Typed('#typed-output', {
      strings: [
        'true cinephiles.',
        'TV show bingers.',
        'actor filmographies.',
        'offline-first lovers.',
        'smooth 120fps UI.'
      ],
      typeSpeed: 50,
      backSpeed: 30,
      backDelay: 2000,
      loop: true
    });
  }

  // 3. Live Poster Theming Engine (Coil Extraction Simulation)
  const themes = {
    teal: { color: '#2DD4BF', glow: 'rgba(45, 212, 191, 0.35)' },
    gold: { color: '#FFCA28', glow: 'rgba(255, 202, 40, 0.35)' },
    purple: { color: '#A855F7', glow: 'rgba(168, 85, 247, 0.35)' },
    crimson: { color: '#F43F5E', glow: 'rgba(244, 63, 94, 0.35)' },
    emerald: { color: '#10B981', glow: 'rgba(16, 185, 129, 0.35)' }
  };

  const themePills = document.querySelectorAll('.theme-pill');
  themePills.forEach(pill => {
    pill.addEventListener('click', (e) => {
      themePills.forEach(p => p.classList.remove('active'));
      pill.classList.add('active');

      const themeKey = pill.getAttribute('data-theme');
      const selected = themes[themeKey] || themes.teal;

      document.documentElement.style.setProperty('--accent-teal', selected.color);
      document.documentElement.style.setProperty('--accent-teal-glow', selected.glow);

      // Mini confetti celebration on theme switch
      if (typeof confetti !== 'undefined') {
        const rect = pill.getBoundingClientRect();
        confetti({
          particleCount: 25,
          spread: 60,
          origin: {
            x: (rect.left + rect.width / 2) / window.innerWidth,
            y: (rect.top + rect.height / 2) / window.innerHeight
          },
          colors: [selected.color, '#ffffff', '#1E293B']
        });
      }
    });
  });

  // 4. Interactive Watchlist Simulator Arena (Room DB & TMDB Catalog)
  const demoMovies = [
    { id: 1, title: 'Dune: Part Two', genre: 'Sci-Fi', year: '2024', rating: '★ 8.5', desc: 'Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators.', bg: 'linear-gradient(135deg, #432b15, #1e130b)' },
    { id: 2, title: 'Breaking Bad', genre: 'Drama', year: '2008-2013', rating: '★ 9.5', desc: 'A chemistry teacher diagnosed with inoperable lung cancer turns to manufacturing meth.', bg: 'linear-gradient(135deg, #113321, #081a10)' },
    { id: 3, title: 'Spider-Man: Across the Spider-Verse', genre: 'Animation', year: '2023', rating: '★ 8.7', desc: 'Miles Morales catapults across the Multiverse, where he encounters a team of Spider-People.', bg: 'linear-gradient(135deg, #4a1525, #190a12)' },
    { id: 4, title: 'Oppenheimer', genre: 'Drama', year: '2023', rating: '★ 8.9', desc: 'The story of American scientist J. Robert Oppenheimer and his role in the development of the atomic bomb.', bg: 'linear-gradient(135deg, #3d2214, #140b06)' },
    { id: 5, title: 'Interstellar', genre: 'Sci-Fi', year: '2014', rating: '★ 8.7', desc: 'A team of explorers travel through a wormhole in space in an attempt to ensure humanity\'s survival.', bg: 'linear-gradient(135deg, #162238, #0a0f1a)' },
    { id: 6, title: 'The Dark Knight', genre: 'Action', year: '2008', rating: '★ 9.0', desc: 'When the menace known as the Joker wreaks havoc and chaos on the people of Gotham, Batman must accept one of the greatest tests.', bg: 'linear-gradient(135deg, #1f242d, #0d1014)' },
    { id: 7, title: 'Cyberpunk: Edgerunners', genre: 'Animation', year: '2022', rating: '★ 8.3', desc: 'A street kid trying to survive in a technology and body modification-obsessed city of the future.', bg: 'linear-gradient(135deg, #401035, #1f0619)' },
    { id: 8, title: 'Inception', genre: 'Action', year: '2010', rating: '★ 8.8', desc: 'A thief who steals corporate secrets through the use of dream-sharing technology is given the inverse task of planting an idea.', bg: 'linear-gradient(135deg, #252b36, #101318)' }
  ];

  const grid = document.getElementById('demoMovieGrid');
  const searchInput = document.getElementById('demoSearchInput');
  const genreBtns = document.querySelectorAll('.genre-btn');
  const badge = document.getElementById('watchlistCountBadge');
  let savedCount = 0;
  let activeGenre = 'All';
  let searchQuery = '';

  const renderGrid = () => {
    if (!grid) return;
    grid.innerHTML = '';

    const filtered = demoMovies.filter(m => {
      const matchGenre = activeGenre === 'All' || m.genre === activeGenre;
      const matchSearch = m.title.toLowerCase().includes(searchQuery.toLowerCase()) || m.genre.toLowerCase().includes(searchQuery.toLowerCase());
      return matchGenre && matchSearch;
    });

    if (filtered.length === 0) {
      grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; padding: 3rem; color: var(--text-muted);">No movies or TV shows found matching your filter.</div>`;
      return;
    }

    filtered.forEach(m => {
      const card = document.createElement('div');
      card.className = 'demo-card';
      card.innerHTML = `
        <div class="demo-card-banner" style="background: ${m.bg};">
          <div class="demo-card-overlay">
            <span class="demo-card-rating">${m.rating}</span>
          </div>
        </div>
        <div class="demo-card-body">
          <div class="demo-card-title">${m.title}</div>
          <div class="demo-card-meta"><span>${m.year}</span> • <span style="color: var(--accent-teal); font-weight: 600;">${m.genre}</span></div>
          <div class="demo-card-desc">${m.desc}</div>
          <div class="demo-card-action">
            <button class="btn-add-folder" data-id="${m.id}">
              <span>+ Folder</span>
            </button>
          </div>
        </div>
      `;
      grid.appendChild(card);
    });

    // Re-init Vanilla-Tilt on newly generated cards
    if (typeof VanillaTilt !== 'undefined') {
      VanillaTilt.init(grid.querySelectorAll('.demo-card'), {
        max: 8,
        speed: 300,
        glare: true,
        'max-glare': 0.2
      });
    }

    // Attach click events to buttons
    grid.querySelectorAll('.btn-add-folder').forEach(btn => {
      btn.addEventListener('click', (e) => {
        if (!btn.classList.contains('saved')) {
          btn.classList.add('saved');
          btn.innerHTML = `<span>✓ Saved in Folder</span>`;
          savedCount++;
          if (badge) badge.innerText = `Saved in Folder: ${savedCount} items`;

          // Trigger confetti explosion on save!
          if (typeof confetti !== 'undefined') {
            const rect = btn.getBoundingClientRect();
            confetti({
              particleCount: 40,
              spread: 70,
              origin: {
                x: (rect.left + rect.width / 2) / window.innerWidth,
                y: (rect.top + rect.height / 2) / window.innerHeight
              },
              colors: ['#2DD4BF', '#FFCA28', '#10B981', '#ffffff']
            });
          }
        } else {
          btn.classList.remove('saved');
          btn.innerHTML = `<span>+ Folder</span>`;
          savedCount = Math.max(0, savedCount - 1);
          if (badge) badge.innerText = `Saved in Folder: ${savedCount} items`;
        }
      });
    });
  };

  if (searchInput) {
    searchInput.addEventListener('input', (e) => {
      searchQuery = e.target.value;
      renderGrid();
    });
  }

  genreBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      genreBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      activeGenre = btn.getAttribute('data-genre');
      renderGrid();
    });
  });

  // Initial render
  renderGrid();
});
