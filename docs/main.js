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
};

// --- PHASE 3: VISUAL FEATURES SWAP IN PLACE ---
const initVisualFeaturesSwap = () => {
  const rows = gsap.utils.toArray('.feature-swap-item');
  if (rows.length < 2) return;

  // Set initial states: First item visible, others hidden and pushed down
  gsap.set(rows[0], { opacity: 1, y: 0, scale: 1, autoAlpha: 1, pointerEvents: 'auto' });
  gsap.set(rows.slice(1), { opacity: 0, y: 80, scale: 0.95, autoAlpha: 0, pointerEvents: 'none' });

  const tl = gsap.timeline({
    scrollTrigger: {
      trigger: '.visual-features',
      start: 'top top',
      end: 'bottom bottom',
      scrub: 0.8,
      pin: '.features-sticky-container'
    }
  });

  // Transition from Row 1 to Row 2:
  tl.to(rows[0], {
    opacity: 0,
    y: -80,
    scale: 0.94,
    autoAlpha: 0,
    pointerEvents: 'none',
    ease: 'power2.inOut',
    duration: 1
  }, 0.8)
  .to(rows[1], {
    opacity: 1,
    y: 0,
    scale: 1,
    autoAlpha: 1,
    pointerEvents: 'auto',
    ease: 'power2.inOut',
    duration: 1
  }, 0.8)

  // Transition from Row 2 to Row 3:
  .to(rows[1], {
    opacity: 0,
    y: -80,
    scale: 0.94,
    autoAlpha: 0,
    pointerEvents: 'none',
    ease: 'power2.inOut',
    duration: 1
  }, 2.4)
  .to(rows[2], {
    opacity: 1,
    y: 0,
    scale: 1,
    autoAlpha: 1,
    pointerEvents: 'auto',
    ease: 'power2.inOut',
    duration: 1
  }, 2.4);
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

// --- PHASE 6: UNIFIED PORTAL ZOOM & TEXT OVERLAY (APPLE KEYNOTE STYLE) ---
const initPortalZoom = () => {
  const stage = document.querySelector('.portal-stage');
  const title = document.querySelector('.portal-title');
  const words = gsap.utils.toArray('.reveal-word');
  const device = document.querySelector('.portal-device');
  const scrim = document.querySelector('.portal-device-scrim');
  const overlay = document.querySelector('.portal-fade-overlay');
  if (!device || !stage || !title) return;

  const tl = gsap.timeline({
    scrollTrigger: {
      trigger: '.portal-zoom-section',
      start: 'top top',
      end: 'bottom bottom',
      scrub: 0.8,
      pin: '.portal-sticky'
    }
  });

  // 1. Words in the center illuminate sequentially right in the middle of the device
  tl.to([words[0], words[1]], {
    color: '#FFFFFF',
    textShadow: '0 0 35px rgba(255, 255, 255, 0.4)',
    stagger: 0.25,
    ease: 'power1.inOut',
    duration: 1
  }, 0)
  .to([words[2], words[3]], {
    color: '#2DD4BF',
    textShadow: '0 0 60px rgba(45, 212, 191, 0.7)',
    stagger: 0.25,
    ease: 'power1.inOut',
    duration: 1
  }, 0.4)

  // 2. While words illuminate, the phone enlarges simultaneously towards the camera
  .to(device, {
    scale: 26,
    borderRadius: 0,
    borderWidth: 0,
    boxShadow: 'none',
    ease: 'power2.inOut',
    duration: 3.5
  }, 0.2)

  // 3. As you fly through the words, the text zooms forward and fades out gracefully
  .to(title, {
    scale: 1.6,
    opacity: 0,
    filter: 'blur(10px)',
    ease: 'power2.in',
    duration: 1
  }, 0.9)

  // 4. Scrim fades away so the movie artwork is crystal clear during immersion
  .to(scrim, {
    opacity: 0,
    ease: 'power1.out',
    duration: 1
  }, 0.8)

  // 5. Toward the end of the zoom, fade into dark to seamlessly transition to the Marquee
  .to(overlay, {
    opacity: 0.98,
    ease: 'power2.in',
    duration: 1.2
  }, 2.3);
};

// Initialize animations (module scripts are deferred automatically)
initHeroAnimations();
initShowcaseAnimations();
initVisualFeaturesSwap();
initPortalZoom();
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
