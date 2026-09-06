// Phase 1: Core Engine & Hero Section

// 1. Initialize Lenis for smooth scrolling
const lenis = new Lenis({
  duration: 1.2,
  easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)), 
  direction: 'vertical',
  gestureDirection: 'vertical',
  smooth: true,
  mouseMultiplier: 1,
  smoothTouch: false,
  touchMultiplier: 2,
  infinite: false,
});

// 2. Sync Lenis with GSAP ScrollTrigger
// Note: We use global variables 'gsap' and 'ScrollTrigger' provided by CDNs in index.html.
gsap.registerPlugin(ScrollTrigger);

lenis.on('scroll', ScrollTrigger.update);

gsap.ticker.add((time) => {
  lenis.raf(time * 1000);
});

gsap.ticker.lagSmoothing(0);

// 3. Hero entrance animations using GSAP
const initHeroAnimations = () => {
  gsap.from('.hero-anim-item', {
    y: 40,
    opacity: 0,
    duration: 1,
    stagger: 0.2,
    ease: 'power3.out',
    delay: 0.2
  });

  // --- HERO FLOATING PILLOWS PARALLAX ---
  gsap.to(".pillow-1", { 
    y: -15, 
    duration: 3.5, 
    repeat: -1, 
    yoyo: true, 
    ease: "sine.inOut" 
  });
  
  gsap.to(".pillow-2", { 
    y: 15, 
    duration: 4.2, 
    repeat: -1, 
    yoyo: true, 
    ease: "sine.inOut", 
    delay: 0.5 
  });
};

// --- PHASE 2: SHOWCASE ANIMATIONS ---
const initShowcaseAnimations = () => {
  // Set initial state
  gsap.set('.phone', { xPercent: 0, yPercent: 0, rotation: 0, scale: 1 });

  const tl = gsap.timeline({
    scrollTrigger: {
      trigger: '.showcase-section',
      start: 'top top',
      end: 'bottom bottom',
      scrub: true,
      pin: '.sticky-container'
    }
  });

  tl.fromTo('.showcase-bg-text', 
    { opacity: 0, scale: 0.8 },
    { opacity: 1, scale: 1, ease: 'none' }, 
    0
  )
  .to('.left-phone', {
    xPercent: -120,
    rotation: -12,
    ease: 'none'
  }, 0)
  .to('.right-phone', {
    xPercent: 120,
    rotation: 12,
    ease: 'none'
  }, 0)
  .to('.center-phone', {
    scale: 1,
    ease: 'none'
  }, 0)
  .to('.phone-label', {
    opacity: 1,
    y: 0,
    duration: 0.5,
    ease: 'power2.out'
  }, 0.2);

  // --- PHASE 3: VISUAL FEATURES REVEAL ---
  const featureRows = gsap.utils.toArray('.feature-row');
  featureRows.forEach((row) => {
    const visual = row.querySelector('.feature-visual');
    const text = row.querySelector('.feature-text');

    gsap.from([visual, text], {
      scrollTrigger: {
        trigger: row,
        start: "top 80%",
      },
      y: 60,
      opacity: 0,
      duration: 1,
      ease: "power3.out",
      stagger: 0.2
    });
  });
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

  const buttons = document.querySelectorAll('.btn-download');

  buttons.forEach(btn => {
    const innerContent = btn.querySelectorAll('.icon-download, span');

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
  // Global event delegation for bulletproof click handling
  document.addEventListener('click', (e) => {
    const question = e.target.closest('.faq-question');
    if (!question) return;

    const parentItem = question.closest('.faq-item');
    if (!parentItem) return;

    const isAlreadyActive = parentItem.classList.contains('active');

    // Close all accordion items
    document.querySelectorAll('.faq-item').forEach((item) => {
      item.classList.remove('active');
      const btn = item.querySelector('.faq-question');
      if (btn) btn.setAttribute('aria-expanded', 'false');
    });

    // If it wasn't active, expand it
    if (!isAlreadyActive) {
      parentItem.classList.add('active');
      question.setAttribute('aria-expanded', 'true');
    }
  });
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

// Initialize animations (module scripts are deferred automatically)
initHeroAnimations();
initShowcaseAnimations();
initNavbarProgress();
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
