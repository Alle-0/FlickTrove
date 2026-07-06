document.addEventListener('DOMContentLoaded', () => {
  // Navbar scroll effect
  const navbar = document.querySelector('.navbar');

  window.addEventListener('scroll', () => {
    if (window.scrollY > 20) {
      navbar.style.background = 'rgba(10, 10, 10, 0.94)';
      navbar.style.boxShadow = '0 10px 30px rgba(0, 0, 0, 0.5)';
      navbar.style.borderColor = 'rgba(45, 212, 191, 0.3)';
    } else {
      navbar.style.background = 'rgba(5, 5, 5, 0.88)';
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

  const lightbox = document.getElementById('screenshot-modal') || document.querySelector('.lightbox-modal');
  const lightboxImg = lightbox ? (lightbox.querySelector('#lightbox-img') || lightbox.querySelector('img')) : null;
  const lightboxClose = lightbox ? lightbox.querySelector('.lightbox-close') : null;
  const lightboxCaption = lightbox ? lightbox.querySelector('#lightbox-caption') : null;

  const openLightbox = (src, alt, captionText) => {
    if (!lightbox) return;
    if (lightboxImg) {
      lightboxImg.src = src;
      lightboxImg.alt = alt || 'Screenshot';
    }
    if (lightboxCaption) {
      lightboxCaption.textContent = captionText || alt || '';
    }
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
      const captionEl = item.querySelector('.screenshot-caption span');
      const captionText = captionEl ? captionEl.textContent : (img ? img.alt : '');
      if (img) {
        openLightbox(img.src, img.alt, captionText);
      }
    });
  });

  if (lightbox) {
    if (lightboxClose) {
      lightboxClose.addEventListener('click', closeLightbox);
    }
    const lightboxBackdrop = lightbox.querySelector('.lightbox-backdrop');
    if (lightboxBackdrop) {
      lightboxBackdrop.addEventListener('click', closeLightbox);
    }
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
  }

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

  // 1. Initialize Vanilla-Tilt on Cards (without holographic glare)
  if (typeof VanillaTilt !== 'undefined') {
    VanillaTilt.init(document.querySelectorAll('.feature-card:not(.deck-scroll-card)'), {
      max: 4,
      speed: 400,
      glare: false,
      'max-glare': 0,
      scale: 1.01
    });
  }

  // 2. Initialize Typed.js Dynamic Typing in Hero (with guaranteed offline vanilla JS fallback)
  const typedEl = document.getElementById('typed-output');
  if (typedEl) {
    const strings = [
      'true cinephiles.',
      'custom neon folders.',
      'TMDB & Trakt sync.',
      '100% offline Room SQLite.',
      'streaming guides.'
    ];
    if (typeof Typed !== 'undefined' || window.Typed) {
      const TypedConstructor = typeof Typed !== 'undefined' ? Typed : window.Typed;
      new TypedConstructor('#typed-output', {
        strings: strings,
        typeSpeed: 50,
        backSpeed: 30,
        backDelay: 2000,
        loop: true
      });
    } else {
      // Vanilla JS Typewriter Fallback if CDN is offline or blocked
      let strIdx = 0;
      let charIdx = 0;
      let isDeleting = false;
      const typeLoop = () => {
        const currentStr = strings[strIdx];
        if (isDeleting) {
          typedEl.textContent = currentStr.substring(0, charIdx - 1);
          charIdx--;
        } else {
          typedEl.textContent = currentStr.substring(0, charIdx + 1);
          charIdx++;
        }
        let typeSpeed = isDeleting ? 30 : 60;
        if (!isDeleting && charIdx === currentStr.length) {
          typeSpeed = 2000;
          isDeleting = true;
        } else if (isDeleting && charIdx === 0) {
          isDeleting = false;
          strIdx = (strIdx + 1) % strings.length;
          typeSpeed = 300;
        }
        setTimeout(typeLoop, typeSpeed);
      };
      setTimeout(typeLoop, 500);
    }
  }

  // 3. Cyberpunk Neon Click Shockwave Ripple
  document.addEventListener('click', (e) => {
    const shockwave = document.createElement('div');
    shockwave.className = 'click-shockwave';
    shockwave.style.left = `${e.clientX}px`;
    shockwave.style.top = `${e.clientY}px`;
    document.body.appendChild(shockwave);
    setTimeout(() => {
      shockwave.remove();
    }, 600);
  });

  // 3.5 Custom Interactive JS Mouse Cursor with Lag & Trail
  const cursorDot = document.querySelector('.cursor-dot');
  const cursorOutline = document.querySelector('.cursor-outline');
  const bgMouseGlow = document.querySelector('.bg-mouse-glow');

  const isDesktopMouse = window.matchMedia('(hover: hover) and (pointer: fine)').matches && !('ontouchstart' in window || navigator.maxTouchPoints > 0);
  if (cursorDot && cursorOutline && isDesktopMouse) {
    document.body.classList.add('custom-cursor-active');

    let mouseX = window.innerWidth / 2;
    let mouseY = window.innerHeight / 2;
    let outlineX = mouseX;
    let outlineY = mouseY;
    let glowX = mouseX;
    let glowY = mouseY;

    window.addEventListener('mousemove', (e) => {
      mouseX = e.clientX;
      mouseY = e.clientY;
      // Dot follows instantly
      cursorDot.style.left = `${mouseX}px`;
      cursorDot.style.top = `${mouseY}px`;
    }, { passive: true });

    // Smooth physics loop with requestAnimationFrame
    const animateCursor = () => {
      // Outline follows with smooth delay (lerp 0.18)
      outlineX += (mouseX - outlineX) * 0.07;
      outlineY += (mouseY - outlineY) * 0.07;
      cursorOutline.style.left = `${outlineX}px`;
      cursorOutline.style.top = `${outlineY}px`;

      // Ambient background glow follows with ultra-smooth heavy delay (lerp 0.08)
      if (bgMouseGlow) {
        glowX += (mouseX - glowX) * 0.08;
        glowY += (mouseY - glowY) * 0.08;
        bgMouseGlow.style.left = `${glowX}px`;
        bgMouseGlow.style.top = `${glowY}px`;
      }

      requestAnimationFrame(animateCursor);
    };
    animateCursor();

    // Hover effect on true interactive elements only (buttons, links, tabs)
    const attachCursorHover = () => {
      const interactiveElements = document.querySelectorAll('a, button, input, .genre-btn, .screenshot-item, .faq-item, .lightbox-close');
      interactiveElements.forEach(el => {
        el.addEventListener('mouseenter', () => cursorOutline.classList.add('hover-active'));
        el.addEventListener('mouseleave', () => cursorOutline.classList.remove('hover-active'));
      });
    };
    attachCursorHover();
    // Re-attach hover when grid updates
    window.attachCursorHover = attachCursorHover;
  }

  /* =========================================
     4. ADVANCED 120FPS SCROLL DYNAMICS ENGINE
        (Hero Parallax Zoom, Velocity Skewing, Sticky Card Stacking)
     ========================================= */
  const heroSection = document.querySelector('.hero');
  const featureCards = document.querySelectorAll('.feature-card');
  const screenshotCards = document.querySelectorAll('.screenshot-item');
  const faqCards = document.querySelectorAll('.faq-item');

  let lastScrollY = window.scrollY;
  let scrollVelocity = 0;
  let smoothVelocity = 0;
  let heroScale = 1;
  let heroOpacity = 1;
  let heroBlur = 0;

  window.addEventListener('scroll', () => {
    const currentScrollY = window.scrollY;
    scrollVelocity = currentScrollY - lastScrollY;
    lastScrollY = currentScrollY;
  }, { passive: true });

  const scrollCards = Array.from(document.querySelectorAll('.deck-scroll-card'));
  const scrollDeckSection = document.querySelector('.scroll-deck-section');

  scrollCards.forEach((card, idx) => {
    card.addEventListener('click', () => {
      if (scrollDeckSection && idx < scrollCards.length - 1) {
        const totalScrollableHeight = scrollDeckSection.offsetHeight - window.innerHeight;
        if (totalScrollableHeight > 0) {
          const targetStep = idx + 1;
          const targetProgress = targetStep / (scrollCards.length - 1);
          const targetScrollY = scrollDeckSection.offsetTop + (targetProgress * totalScrollableHeight);
          window.scrollTo({ top: targetScrollY, behavior: 'smooth' });
        }
      } else if (scrollDeckSection && idx === scrollCards.length - 1) {
        window.scrollTo({ top: scrollDeckSection.offsetTop, behavior: 'smooth' });
      }
    });
  });

  const animateScrollDynamics = () => {
    // Smooth velocity lerp for organic inertia
    smoothVelocity += (scrollVelocity - smoothVelocity) * 0.12;
    scrollVelocity *= 0.85; // automatic decay when stopped

    // 4.1 Velocity Skewing / Aerodynamic Inertia on Cards
    if (Math.abs(smoothVelocity) > 0.15) {
      const skewAngle = Math.max(Math.min(smoothVelocity * 0.045, 2.0), -2.0);
      const stretchY = 1 + Math.min(Math.abs(smoothVelocity) * 0.0005, 0.02);

      featureCards.forEach(card => {
        if (!card.classList.contains('deck-scroll-card') && !card.matches(':hover')) {
          card.style.transform = `perspective(1000px) skewY(${skewAngle.toFixed(2)}deg) scaleY(${stretchY.toFixed(4)})`;
        }
      });
      screenshotCards.forEach(item => {
        if (!item.matches(':hover')) {
          item.style.transform = `perspective(1000px) skewY(${(skewAngle * 0.6).toFixed(2)}deg)`;
        }
      });
    } else {
      featureCards.forEach(card => {
        if (!card.classList.contains('deck-scroll-card') && !card.matches(':hover') && card.style.transform.includes('skewY')) {
          card.style.transform = '';
        }
      });
      screenshotCards.forEach(item => {
        if (!item.matches(':hover') && item.style.transform.includes('skewY')) {
          item.style.transform = '';
        }
      });
    }

    // 4.2 Hero Section Deep 3D Parallax & Depth Fade
    const heroSection = document.querySelector('.hero');
    if (heroSection && window.scrollY < 850) {
      const scrollY = window.scrollY;
      const targetTranslateY = scrollY * 0.65;
      heroScale = Math.max(0.88, 1 - scrollY * 0.00025);
      heroOpacity = Math.max(0, 1 - scrollY * 0.0014);
      heroBlur = Math.min(12, scrollY * 0.012);

      heroSection.style.transform = `translate3d(0, ${targetTranslateY.toFixed(1)}px, 0) scale(${heroScale.toFixed(4)})`;
      heroSection.style.opacity = Math.max(0, heroOpacity).toFixed(3);
      heroSection.style.filter = `blur(${heroBlur.toFixed(1)}px)`;
    } else if (heroSection && window.scrollY >= 850) {
      heroSection.style.opacity = '0';
    }

    // 4.3 Scroll-Controlled 3D Cinematic Deck of 9 Cards
    if (scrollDeckSection && scrollCards.length > 0) {
      const rect = scrollDeckSection.getBoundingClientRect();
      const windowHeight = window.innerHeight;
      const totalScrollableHeight = rect.height - windowHeight;

      let progress = 0;
      if (totalScrollableHeight > 0) {
        progress = Math.max(0, Math.min(1, -rect.top / totalScrollableHeight));
      }

      const numCards = scrollCards.length;
      const totalSteps = numCards - 1;
      const currentStep = progress * totalSteps;

      const activeIdx = Math.floor(currentStep);
      const stepP = currentStep - activeIdx; // 0.0 to 1.0 within current step

      // Dwell zone: 55% of each step is locked motionless for comfortable reading.
      // 45% is the smooth swipe transition to the next card.
      const dwell = 0.55;
      const trans = 1.0 - dwell;
      const t = stepP <= dwell ? 0 : (stepP - dwell) / trans; // 0 during dwell, 0->1 during transition

      scrollCards.forEach((card, i) => {
        let effectiveOffset;
        if (i < activeIdx) {
          effectiveOffset = -1; // Already dealt away
        } else if (i === activeIdx) {
          effectiveOffset = -t; // 0 (frozen in reading zone!), then moves 0 -> -1 during transition
        } else {
          const baseDepth = i - activeIdx;
          effectiveOffset = baseDepth - t; // e.g. Card 1 rests at depth 1, then moves 1 -> 0 during transition
        }

        if (effectiveOffset <= -0.98) {
          // Dealt away off screen
          card.style.transform = `translate3d(125%, 20%, 0) rotate(22deg) scale(0.85)`;
          card.style.opacity = '0';
          card.style.zIndex = '0';
          card.style.pointerEvents = 'none';
        } else if (effectiveOffset < 0) {
          // Currently dealing out! (swiping sideways and slightly down, never up into the header text)
          const p = -effectiveOffset; // goes from 0 to 1
          const translateY = p * 15;
          const translateX = p * 130;
          const rotate = p * 18;
          const scale = 1 - p * 0.15;
          // Stay 100% solid opaque for the first part of the exit swipe, fade out only at the end
          const opacity = p < 0.35 ? 1 : Math.max(0, 1 - ((p - 0.35) / 0.65));

          card.style.transform = `translate3d(${translateX.toFixed(1)}%, ${translateY.toFixed(1)}%, 0) rotate(${rotate.toFixed(1)}deg) scale(${scale.toFixed(3)})`;
          card.style.opacity = opacity.toFixed(3);
          card.style.zIndex = `${numCards + 5}`;
          card.style.pointerEvents = 'none';
        } else if (effectiveOffset === 0) {
          // Active top card locked in reading zone!
          card.style.transform = `translate3d(0, 0, 0) rotate(0deg) scale(1)`;
          card.style.opacity = '1';
          card.style.zIndex = `${numCards}`;
          card.style.pointerEvents = 'auto';
        } else {
          // Waiting in the deck behind the top card!
          const depth = Math.min(effectiveOffset, 5); // limit visible stacking depth
          const translateY = depth * 24; // 24px down per step
          const scale = Math.max(0.75, 1 - depth * 0.045);
          const rotate = ((i % 2 === 0) ? -1 : 1) * depth * 2.2;
          // ZERO TRANSPARENCY! Stack cards are 100% opaque solid physical cards!
          const opacity = depth <= 2.5 ? 1 : Math.max(0, 1 - (depth - 2.5) * 0.4);

          card.style.transform = `translate3d(0, ${translateY.toFixed(1)}px, 0) rotate(${rotate.toFixed(1)}deg) scale(${scale.toFixed(3)})`;
          card.style.opacity = opacity.toFixed(3);
          card.style.zIndex = `${Math.max(1, numCards - Math.floor(effectiveOffset))}`;
          card.style.pointerEvents = effectiveOffset <= 1 ? 'auto' : 'none';
        }
      });
    }

    requestAnimationFrame(animateScrollDynamics);
  };
  animateScrollDynamics();

});
