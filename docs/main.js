// --- FLICKTROVE CINEMATIC ANIMATION ENGINE ---
// Inspired by Awwwards-winning choreography (Lando Norris, Don't Board Me, Igloo Inc)

// Detect touch/mobile environment to preserve 100% native 60/120 FPS momentum scroll
const isTouchDevice = ('ontouchstart' in window) || navigator.maxTouchPoints > 0 || window.innerWidth <= 768;

// 1. Sync Lenis with GSAP ScrollTrigger
gsap.registerPlugin(ScrollTrigger);
ScrollTrigger.config({ 
  ignoreMobileResize: true,
  fastScrollEnd: true,
  syncInterval: 30
});

// 2. Initialize Lenis ONLY on desktop mouse pointer devices
let lenis = null;
if (!isTouchDevice && typeof Lenis !== 'undefined') {
  lenis = new Lenis({
    duration: 1.2,
    easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)), 
    direction: 'vertical',
    smooth: true,
    mouseMultiplier: 1,
    infinite: false,
  });

  lenis.on('scroll', ScrollTrigger.update);

  gsap.ticker.add((time) => {
    lenis.raf(time * 1000);
  });
}

gsap.ticker.lagSmoothing(500, 33);

// 3. Hero entrance animations using GSAP
const initHeroAnimations = () => {
  gsap.from('.hero-anim-item', {
    y: 40,
    opacity: 0,
    duration: 1,
    stagger: 0.18,
    ease: 'power3.out',
    delay: 0.2
  });

  // Hero phones entrance stagger
  gsap.from(['.comp-center', '.comp-left', '.comp-right'], {
    scale: 0.85,
    y: 50,
    opacity: 0,
    duration: 1.2,
    stagger: 0.15,
    ease: 'power3.out',
    delay: 0.35
  });

  // --- HERO FLOATING PILLOWS PARALLAX ---
  gsap.to(".pillow-1", { 
    y: -18, 
    duration: 3.5, 
    repeat: -1, 
    yoyo: true, 
    ease: "sine.inOut" 
  });
  
  gsap.to(".pillow-2", { 
    y: 18, 
    duration: 4.2, 
    repeat: -1, 
    yoyo: true, 
    ease: "sine.inOut", 
    delay: 0.5 
  });

  // Scrubbed Hero Exit Parallax on Scroll (desktop only for buttery 60 FPS mobile scroll)
  if (!isTouchDevice) {
    gsap.to('.hero-text-col', {
      yPercent: -20,
      opacity: 0.2,
      ease: 'none',
      scrollTrigger: {
        trigger: '.hero',
        start: 'top top',
        end: 'bottom top',
        scrub: 1
      }
    });

    gsap.to('.hero-visual-col', {
      yPercent: -12,
      scale: 0.96,
      ease: 'none',
      scrollTrigger: {
        trigger: '.hero',
        start: 'top top',
        end: 'bottom top',
        scrub: 1
      }
    });
  }
};

// --- HERO 3D INTERACTIVE PERSPECTIVE PARALLAX (IGLOO INC & LANDO NORRIS) ---
const init3DHeroParallax = () => {
  if (!window.matchMedia('(hover: hover) and (pointer: fine)').matches) return;

  const heroVisual = document.querySelector('.hero-visual-col');
  const centerPhone = document.querySelector('.comp-center');
  const leftPhone = document.querySelector('.comp-left');
  const rightPhone = document.querySelector('.comp-right');
  const pillow1 = document.querySelector('.pillow-1');
  const pillow2 = document.querySelector('.pillow-2');

  if (!heroVisual || !centerPhone) return;

  heroVisual.addEventListener('mousemove', (e) => {
    const rect = heroVisual.getBoundingClientRect();
    const nx = (e.clientX - rect.left) / rect.width - 0.5; // -0.5 to +0.5
    const ny = (e.clientY - rect.top) / rect.height - 0.5;

    // 3D perspective tilt on center phone
    gsap.to(centerPhone, {
      rotateY: nx * 18,
      rotateX: -ny * 18,
      x: nx * 28,
      y: ny * 20,
      duration: 0.5,
      ease: 'power2.out'
    });

    // Opposing depth offsets on side phones
    if (leftPhone) {
      gsap.to(leftPhone, {
        rotateY: nx * 14 - 6,
        rotateX: -ny * 14,
        x: -160 + nx * 18,
        y: -20 + ny * 15,
        duration: 0.6,
        ease: 'power2.out'
      });
    }

    if (rightPhone) {
      gsap.to(rightPhone, {
        rotateY: nx * 14 + 6,
        rotateX: -ny * 14,
        x: 160 + nx * 18,
        y: 40 + ny * 15,
        duration: 0.6,
        ease: 'power2.out'
      });
    }

    // Floating pillows reactive parallax
    if (pillow1) {
      gsap.to(pillow1, {
        x: nx * 50,
        y: ny * 40,
        duration: 0.4,
        ease: 'power2.out'
      });
    }

    if (pillow2) {
      gsap.to(pillow2, {
        x: nx * 60,
        y: ny * 45,
        duration: 0.4,
        ease: 'power2.out'
      });
    }
  });

  heroVisual.addEventListener('mouseleave', () => {
    gsap.to([centerPhone, leftPhone, rightPhone, pillow1, pillow2], {
      rotateX: 0,
      rotateY: 0,
      duration: 1.2,
      ease: 'elastic.out(1, 0.4)'
    });
  });
};

// --- PHASE 2: SHOWCASE ANIMATIONS (PERFORMANCE-OPTIMIZED) ---
const initShowcaseAnimations = () => {
  const isMobile = window.innerWidth <= 768;
  const xOffset = isMobile ? 65 : 130;
  const rotAngle = isMobile ? 6 : 13;

  gsap.set('.phone', { xPercent: 0, yPercent: 0, rotation: 0, scale: 1, force3D: true });

  const tl = gsap.timeline({
    scrollTrigger: {
      trigger: '.showcase-section',
      start: 'top top',
      end: 'bottom bottom',
      scrub: isMobile ? true : 1,
      pin: '.sticky-container',
      anticipatePin: 1
    }
  });

  // 1. Background typography fades and scales in behind the phones
  tl.fromTo('.showcase-bg-text', 
    { opacity: isMobile ? 0.3 : 0, scale: isMobile ? 0.95 : 0.8 }, 
    { opacity: 1, scale: 1, ease: 'none' }, 
    0
  )
  // 2. Left phone fans out (avoid expensive rotateY on mobile)
  .to('.left-phone', {
    xPercent: -xOffset,
    rotation: -rotAngle,
    rotateY: isMobile ? 0 : 18,
    scale: 0.94,
    ease: 'power1.out'
  }, 0)
  // 3. Right phone fans out in opposite rotation
  .to('.right-phone', {
    xPercent: xOffset,
    rotation: rotAngle,
    rotateY: isMobile ? 0 : -18,
    scale: 0.94,
    ease: 'power1.out'
  }, 0)
  // 4. Center phone lifts forward towards camera
  .to('.center-phone', {
    scale: isMobile ? 1.02 : 1.05,
    z: isMobile ? 0 : 40,
    ease: 'power1.out'
  }, 0)
  // 5. Phone labels reveal
  .to('.phone-label', {
    opacity: 1,
    y: 0,
    stagger: isMobile ? 0.04 : 0.08,
    duration: 0.5,
    ease: 'back.out(1.5)'
  }, 0.25);

  // Interactive 3D mouse hover on the 3 showcase phones while pinned (desktop only)
  if (window.matchMedia('(hover: hover) and (pointer: fine)').matches) {
    const phones = document.querySelectorAll('.phone');
    phones.forEach(p => {
      p.addEventListener('mouseenter', () => {
        gsap.to(p, { scale: 1.06, duration: 0.35, ease: 'power2.out' });
      });
      p.addEventListener('mouseleave', () => {
        gsap.to(p, { scale: 1, duration: 0.4, ease: 'power2.out' });
      });
    });
  }
};

// --- PHASE 3: CORE CAPABILITIES TAB CAROUSEL (FLICKTROVE SLIDING PILL) ---
const initFeaturesTabsCarousel = () => {
  const section = document.querySelector('.features-carousel-section');
  const tabsBar = document.querySelector('.features-tabs-bar');
  const tabBtns = document.querySelectorAll('.feature-tab-btn');
  const panels = document.querySelectorAll('.feature-panel');
  const glow = document.getElementById('featuresAmbientGlow');
  const slidingPill = document.getElementById('tabSlidingPill');
  if (!tabBtns.length || !panels.length) return;

  // Dynamic ambient glows per tab
  const glows = [
    'radial-gradient(circle, rgba(45, 212, 191, 0.22) 0%, transparent 70%)',
    'radial-gradient(circle, rgba(229, 9, 20, 0.22) 0%, transparent 70%)',
    'radial-gradient(circle, rgba(16, 185, 129, 0.22) 0%, transparent 70%)'
  ];

  let currentTab = 0;
  let isAnimating = false;

  const moveSlidingPill = (targetBtn, index, immediate = false) => {
    if (!slidingPill || !targetBtn) return;
    const targetX = targetBtn.offsetLeft;
    const targetWidth = targetBtn.offsetWidth;

    // Theme color class on the sliding pill
    slidingPill.classList.remove('color-tab', 'offline-tab');
    if (index === 1) slidingPill.classList.add('color-tab');
    if (index === 2) slidingPill.classList.add('offline-tab');

    if (immediate) {
      gsap.set(slidingPill, { x: targetX, width: targetWidth });
    } else {
      gsap.to(slidingPill, {
        x: targetX,
        width: targetWidth,
        duration: 0.38,
        ease: 'power3.out'
      });
    }
  };

  const switchTab = (index, immediate = false) => {
    if (index === currentTab && !immediate) return;
    if (isAnimating && !immediate) return;

    const prevPanel = panels[currentTab];
    const nextPanel = panels[index];
    currentTab = index;

    // Smoothly animate the sliding pill indicator
    moveSlidingPill(tabBtns[index], index, immediate);

    // Update tab button states
    tabBtns.forEach((btn, i) => {
      const isActive = i === index;
      btn.classList.toggle('active', isActive);
      btn.setAttribute('aria-selected', isActive ? 'true' : 'false');
    });

    // Update ambient glow
    if (glow && glows[index]) {
      glow.style.background = glows[index];
    }

    if (immediate) {
      panels.forEach((p, i) => {
        p.classList.toggle('active', i === index);
        p.style.opacity = i === index ? 1 : 0;
      });
      return;
    }

    isAnimating = true;

    // Smooth cross-fade transition
    gsap.to(prevPanel, {
      opacity: 0,
      y: -10,
      scale: 0.98,
      duration: 0.25,
      ease: 'power2.in',
      onComplete: () => {
        prevPanel.classList.remove('active');
        nextPanel.classList.add('active');
        gsap.fromTo(
          nextPanel,
          { opacity: 0, y: 15, scale: 0.98 },
          {
            opacity: 1,
            y: 0,
            scale: 1,
            duration: 0.38,
            ease: 'power2.out',
            onComplete: () => {
              isAnimating = false;
            }
          }
        );
      }
    });
  };

  // Add click listeners to tab buttons
  tabBtns.forEach((btn, i) => {
    btn.addEventListener('click', (e) => {
      e.preventDefault();
      switchTab(i);
      if (window.innerWidth <= 768) {
        btn.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
      }
    });
  });

  // Initial indicator position after fonts & layout settle
  const initPillPos = () => {
    moveSlidingPill(tabBtns[0], 0, true);
  };
  initPillPos();
  window.addEventListener('load', initPillPos);
  window.addEventListener('resize', () => {
    moveSlidingPill(tabBtns[currentTab], currentTab, true);
  });

  // Entrance scroll animation
  if (section) {
    gsap.from(['.features-section-header', '.features-tabs-bar', '.features-stage-wrapper'], {
      y: 35,
      opacity: 0,
      duration: 0.8,
      stagger: 0.15,
      ease: 'power3.out',
      scrollTrigger: {
        trigger: section,
        start: 'top 80%',
        once: true
      }
    });
  }
};

// --- NAVBAR PROGRESS ANIMATION ---
const initNavbarProgress = () => {
  gsap.to('.scroll-progress-bar', {
    width: '100%',
    ease: 'none',
    scrollTrigger: {
      trigger: document.documentElement,
      start: 'top top',
      end: 'bottom bottom',
      scrub: true
    }
  });
};

const initCTAAnimation = () => {
  gsap.from('.final-cta-section > *', {
    y: 50,
    opacity: 0,
    duration: 1,
    stagger: 0.1,
    ease: "power3.out",
    scrollTrigger: {
      trigger: '.final-cta-section',
      start: 'top 80%'
    }
  });
};

// --- MAGNETIC CTA BUTTON EFFECT (Vercel & Linear style) ---
const initMagneticCTA = () => {
  // Only enable on desktop with fine mouse pointer
  if (!window.matchMedia('(hover: hover) and (pointer: fine)').matches) return;

  const buttons = document.querySelectorAll('.btn-download, .btn-secondary-glass, .support-chip, .support-btn');

  buttons.forEach(btn => {
    const innerContent = btn.querySelectorAll('.icon-download, svg, span, .chip-icon, .chip-amount, .chip-label');

    btn.addEventListener('mousemove', (e) => {
      const rect = btn.getBoundingClientRect();
      const x = e.clientX - (rect.left + rect.width / 2);
      const y = e.clientY - (rect.top + rect.height / 2);

      // Magnetic physical attraction towards cursor
      gsap.to(btn, {
        x: x * 0.35,
        y: y * 0.35,
        duration: 0.3,
        ease: 'power2.out'
      });

      // Subtle depth parallax on inner content
      gsap.to(innerContent, {
        x: x * 0.15,
        y: y * 0.15,
        duration: 0.3,
        ease: 'power2.out'
      });
    });

    btn.addEventListener('mouseleave', () => {
      // Elastic spring snap-back to origin
      gsap.to(btn, {
        x: 0,
        y: 0,
        scale: 1,
        duration: 0.8,
        ease: 'elastic.out(1.2, 0.4)'
      });

      gsap.to(innerContent, {
        x: 0,
        y: 0,
        duration: 0.7,
        ease: 'elastic.out(1.1, 0.4)'
      });
    });

    // Tactile micro-bounce on click
    btn.addEventListener('mousedown', () => {
      gsap.to(btn, {
        scale: 0.93,
        duration: 0.1,
        ease: 'power2.out'
      });
    });

    btn.addEventListener('mouseup', () => {
      gsap.to(btn, {
        scale: 1.02,
        duration: 0.45,
        ease: 'elastic.out(1.4, 0.35)'
      });
    });
  });
};

// --- PHASE 5: FAQ ACCORDION LOGIC ---
const initFAQ = () => {
  // Ensure window.toggleFAQ is available
  if (!window.toggleFAQ) {
    window.toggleFAQ = (btn, event) => {
      if (event) {
        event.preventDefault();
        event.stopPropagation();
      }
      const item = btn.closest('.faq-item');
      if (!item) return;

      const wasActive = item.classList.contains('active');

      document.querySelectorAll('.faq-item').forEach((el) => {
        el.classList.remove('active');
        const b = el.querySelector('.faq-question');
        if (b) b.setAttribute('aria-expanded', 'false');
      });

      if (!wasActive) {
        item.classList.add('active');
        btn.setAttribute('aria-expanded', 'true');
      }
    };
  }
};

const initFAQAnimation = () => {
  gsap.from('.faq-header', {
    y: 30,
    opacity: 0,
    duration: 0.8,
    ease: 'power3.out',
    scrollTrigger: {
      trigger: '.faq-section',
      start: 'top 90%',
      once: true
    }
  });
};

// --- PHASE 6: UNIFIED PORTAL ZOOM & TEXT OVERLAY (MOBILE 60 FPS OPTIMIZED) ---
const initPortalZoom = () => {
  const stage = document.querySelector('.portal-stage');
  const title = document.querySelector('.portal-title');
  const words = gsap.utils.toArray('.reveal-word');
  const device = document.querySelector('.portal-device');
  const deviceImg = document.querySelector('.portal-device img');
  const scrim = document.querySelector('.portal-device-scrim');
  if (!device || !stage || !title) return;

  const isMobile = window.innerWidth <= 768;
  const targetScale = isMobile ? 4.5 : 24;
  const scrubSpeed = isMobile ? true : 1.2;

  gsap.set(device, { xPercent: -50, yPercent: -50, force3D: true });

  const tl = gsap.timeline({
    scrollTrigger: {
      trigger: '.portal-zoom-section',
      start: 'top top',
      end: 'bottom bottom',
      scrub: scrubSpeed,
      pin: '.portal-sticky',
      anticipatePin: 1
    }
  });

  // 1. Words illuminate (no heavy textShadow blurs on mobile)
  tl.to([words[0], words[1]], {
    color: '#FFFFFF',
    textShadow: isMobile ? 'none' : '0 0 35px rgba(255, 255, 255, 0.4)',
    stagger: 0.25,
    ease: 'power1.inOut',
    duration: 0.9
  }, 0)
  .to([words[2], words[3]], {
    color: '#2DD4BF',
    textShadow: isMobile ? 'none' : '0 0 50px rgba(45, 212, 191, 0.6)',
    stagger: 0.25,
    ease: 'power1.inOut',
    duration: 0.9
  }, 0.9)
  .to([words[4], words[5]], {
    color: '#2DD4BF',
    textShadow: isMobile ? 'none' : '0 0 70px rgba(45, 212, 191, 0.85)',
    stagger: 0.25,
    ease: 'power1.inOut',
    duration: 1.0
  }, 1.8)

  // 2. Phone enlarges towards camera
  .to(device, {
    scale: targetScale,
    xPercent: -50,
    yPercent: -50,
    borderRadius: 0,
    borderWidth: 0,
    boxShadow: 'none',
    backgroundColor: 'transparent',
    ease: 'power2.inOut',
    duration: 3.2
  }, 0.2);

  // 3. Image dissolves (pure opacity on mobile, no filter: blur)
  const imgFade = {
    opacity: 0,
    ease: 'power2.inOut',
    duration: 2.0
  };
  if (!isMobile) {
    imgFade.filter = 'blur(24px)';
  }
  tl.to(deviceImg, imgFade, 0.8)

  // 4. Scrim fades cleanly
  .to(scrim, {
    opacity: 0,
    ease: 'power1.out',
    duration: 1.5
  }, 0.8)

  // 5. Title subtle float (desktop only)
  .to(title, {
    scale: isMobile ? 1 : 1.08,
    ease: 'power1.out',
    duration: 3.0
  }, 0.5);
};

// --- SVG VECTOR STROKE DRAW (LANDO NORRIS SIGNATURE EFFECT) ---
const initSVGPathDrawing = () => {
  const path = document.querySelector('.nav-logo-path');
  if (path && path.getTotalLength) {
    const len = path.getTotalLength();
    gsap.set(path, { strokeDasharray: len, strokeDashoffset: len });
    gsap.to(path, {
      strokeDashoffset: 0,
      duration: 2.2,
      ease: 'power3.inOut',
      delay: 0.3
    });
  }
};

// Initialize animations
initHeroAnimations();
init3DHeroParallax();
initShowcaseAnimations();
initFeaturesTabsCarousel();
initPortalZoom();
initNavbarProgress();
initSVGPathDrawing();
initFAQ();
initFAQAnimation();
initCTAAnimation();
initMagneticCTA();

// --- MOBILE MENU LOGIC ---
const hamburgerBtn = document.getElementById('hamburger-btn');
const mobileMenu = document.getElementById('mobile-menu');
const mobileMenuClose = document.getElementById('mobile-menu-close');
const mobileLinks = document.querySelectorAll('.mobile-link');

if (hamburgerBtn && mobileMenu) {
  hamburgerBtn.addEventListener('click', () => {
    mobileMenu.classList.toggle('active');
    document.body.style.overflow = mobileMenu.classList.contains('active') ? 'hidden' : '';
  });

  if (mobileMenuClose) {
    mobileMenuClose.addEventListener('click', () => {
      mobileMenu.classList.remove('active');
      document.body.style.overflow = '';
    });
  }

  mobileLinks.forEach(link => {
    link.addEventListener('click', () => {
      mobileMenu.classList.remove('active');
      document.body.style.overflow = '';
    });
  });
}
