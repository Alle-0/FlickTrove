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

// Initialize animations (module scripts are deferred automatically)
initHeroAnimations();
initShowcaseAnimations();
initNavbarProgress();
initCTAAnimation();
