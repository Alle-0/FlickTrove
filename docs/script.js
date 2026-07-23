document.addEventListener('DOMContentLoaded', () => {
  // Navbar scroll effect
  const navbar = document.querySelector('.navbar');

  // Scroll Laser Progress Bar
  const scrollLaser = document.getElementById('scroll-laser');
  const updateScrollLaser = () => {
    if (scrollLaser) {
      const totalHeight = document.documentElement.scrollHeight - window.innerHeight;
      const progress = totalHeight > 0 ? (window.scrollY / totalHeight) * 100 : 0;
      scrollLaser.style.width = `${Math.min(100, Math.max(0, progress))}%`;
    }
  };
  updateScrollLaser();

  window.addEventListener('scroll', () => {
    updateScrollLaser();
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
        mobileBtn.classList.remove('active');
        mobileBtn.setAttribute('aria-expanded', 'false');
        document.documentElement.classList.remove('drawer-open');
        document.body.classList.remove('drawer-open');
      } else {
        mobileDrawer.classList.add('active');
        mobileBtn.classList.add('active');
        mobileBtn.setAttribute('aria-expanded', 'true');
        document.documentElement.classList.add('drawer-open');
        document.body.classList.add('drawer-open');
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
      if (mobileDrawer.classList.contains('active') && !mobileDrawer.contains(e.target) && !mobileBtn.contains(e.target)) {
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
      '100% offline use.',
      'visual perfection.',
      'seamless cloud sync.',
      'finding where to stream.',
      'you.'
    ];
    if (typeof Typed !== 'undefined' || window.Typed) {
      const TypedConstructor = typeof Typed !== 'undefined' ? Typed : window.Typed;
      new TypedConstructor('#typed-output', {
        strings: strings,
        typeSpeed: 45,
        backSpeed: 25,
        backDelay: 3500,
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
        let typeSpeed = isDeleting ? 25 : 45;
        if (!isDeleting && charIdx === currentStr.length) {
          typeSpeed = 3500;
          isDeleting = true;
        } else if (isDeleting && charIdx === 0) {
          isDeleting = false;
          strIdx = (strIdx + 1) % strings.length;
          typeSpeed = 400;
        }
        setTimeout(typeLoop, typeSpeed);
      };
      setTimeout(typeLoop, 500);
    }
  }

  // 2.5 3D Mouse Parallax on Hero Visual Stage (Overlapping Smartphones)
  const visualStage = document.querySelector('.hero-visual-stage');
  const phoneMain = document.querySelector('.phone-mockup-main');
  const phoneBg = document.querySelector('.phone-mockup-bg');
  if (visualStage && phoneMain && phoneBg && window.innerWidth > 968) {
    visualStage.addEventListener('mousemove', (e) => {
      const rect = visualStage.getBoundingClientRect();
      const x = ((e.clientX - rect.left) / rect.width) - 0.5;
      const y = ((e.clientY - rect.top) / rect.height) - 0.5;

      phoneMain.style.transform = `rotate(${-3 + x * 8}deg) rotateX(${-y * 10}deg) translateZ(40px)`;
      phoneBg.style.transform = `rotate(${6 - x * 6}deg) rotateX(${y * 8}deg) scale(0.92) translateZ(-20px)`;
    });

    visualStage.addEventListener('mouseleave', () => {
      phoneMain.style.transform = '';
      phoneBg.style.transform = '';
    });
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
  let filmstripPosLeft = 0;
  let filmstripPosRight = 0;
  let heroScale = 1;
  let heroOpacity = 1;
  let heroBlur = 0;

  // 3D Circular Orbit Carousel State
  let carouselAutoAngle = 0;
  let isCarouselHovered = false;
  let currentCarouselSpeed = 1.0;
  let lastFrameTimestamp = performance.now();

  let snapTimeout;
  window.addEventListener('scroll', () => {
    const currentScrollY = window.scrollY;
    scrollVelocity = currentScrollY - lastScrollY;
    lastScrollY = currentScrollY;

    // Magnetic Snap Logic
    clearTimeout(snapTimeout);
    snapTimeout = setTimeout(() => {
      if (scrollDeckSection && scrollCards.length > 0) {
        const rect = scrollDeckSection.getBoundingClientRect();
        // Check if deck is active in viewport
        if (rect.top <= 0 && rect.bottom >= window.innerHeight) {
          const totalScrollableHeight = rect.height - window.innerHeight;
          if (totalScrollableHeight > 0) {
            const progress = Math.max(0, Math.min(1, -rect.top / totalScrollableHeight));
            const numCards = scrollCards.length;
            const exactCardPos = progress * (numCards - 1);
            const nearestIdx = Math.round(exactCardPos);
            
            // Snap to the nearest card
            const targetTop = window.scrollY + rect.top + (nearestIdx / Math.max(1, numCards - 1)) * totalScrollableHeight;
            if (Math.abs(window.scrollY - targetTop) > 5) {
              if (window.lenis) {
                window.lenis.scrollTo(targetTop, { duration: 0.8, easing: (t) => 1 - Math.pow(1 - t, 4) });
              } else {
                window.scrollTo({ top: targetTop, behavior: 'smooth' });
              }
            }
          }
        }
      }
    }, 200);
  }, { passive: true });

  const scrollCards = Array.from(document.querySelectorAll('.deck-scroll-card'));
  const scrollDeckSection = document.querySelector('.scroll-deck-section');
  const filmstripHoles = document.querySelectorAll('.filmstrip-divider .film-holes');
  const filmstripTracks = document.querySelectorAll('.filmstrip-divider .film-line');
  const deckDots = Array.from(document.querySelectorAll('.deck-sidebar-dots .deck-dot'));

  deckDots.forEach((dot, idx) => {
    dot.addEventListener('click', () => {
      if (scrollDeckSection && scrollCards.length > 0) {
        const rect = scrollDeckSection.getBoundingClientRect();
        const totalScrollableHeight = rect.height - window.innerHeight;
        if (totalScrollableHeight > 0) {
          const numCards = scrollCards.length;
          const targetTop = window.scrollY + rect.top + (idx / Math.max(1, numCards - 1)) * totalScrollableHeight;
          if (window.lenis) {
            window.lenis.scrollTo(targetTop, { duration: 1.0 });
          } else {
            window.scrollTo({ top: targetTop, behavior: 'smooth' });
          }
        }
      }
    });
  });

  scrollCards.forEach((card, idx) => {
    card.addEventListener('click', () => {
      if (scrollDeckSection && scrollCards.length > 0 && card.style.pointerEvents !== 'auto') {
        const rect = scrollDeckSection.getBoundingClientRect();
        const totalScrollableHeight = rect.height - window.innerHeight;
        if (totalScrollableHeight > 0) {
          const numCards = scrollCards.length;
          const targetTop = window.scrollY + rect.top + (idx / Math.max(1, numCards - 1)) * totalScrollableHeight;
          if (window.lenis) {
            window.lenis.scrollTo(targetTop, { duration: 1.0 });
          } else {
            window.scrollTo({ top: targetTop, behavior: 'smooth' });
          }
        }
      }
    });
  });

  const animateScrollDynamics = () => {
    // Smooth velocity lerp for organic inertia
    smoothVelocity += (scrollVelocity - smoothVelocity) * 0.12;
    scrollVelocity *= 0.85; // automatic decay when stopped

    // 4.1.5 Cinematic Double Filmstrip Counter-Scrolling (Twin Reel Marquee)
    const stripLeftElements = document.querySelectorAll('.strip-left .film-holes, .strip-left .film-line');
    const stripRightElements = document.querySelectorAll('.strip-right .film-holes, .strip-right .film-line');
    if (stripLeftElements.length > 0 || stripRightElements.length > 0) {
      const scrollBoost = Math.min(Math.abs(smoothVelocity) * 0.15, 1.2);
      filmstripPosLeft -= (0.25 + scrollBoost);
      filmstripPosRight += (0.18 + scrollBoost * 0.85);

      stripLeftElements.forEach((el) => {
        el.style.backgroundPosition = `${filmstripPosLeft.toFixed(2)}px 0px`;
      });
      stripRightElements.forEach((el) => {
        el.style.backgroundPosition = `${filmstripPosRight.toFixed(2)}px 0px`;
      });
    }

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
    if (heroSection) {
      // Disabilita l'animazione parallasse pesante su dispositivi mobile
      const isMobile = window.innerWidth <= 768;

      if (!isMobile && window.scrollY < 850) {
        const scrollY = window.scrollY;
        const targetTranslateY = scrollY * 0.8;
        heroScale = Math.max(0.88, 1 - scrollY * 0.00025);
        heroOpacity = Math.max(0, 1 - scrollY * 0.0014);
        heroBlur = Math.min(12, scrollY * 0.0075);

        heroSection.style.transform = `translate3d(0, ${targetTranslateY.toFixed(1)}px, 0) scale(${heroScale.toFixed(4)})`;
        heroSection.style.opacity = Math.max(0, heroOpacity).toFixed(3);
        heroSection.style.filter = `blur(${heroBlur.toFixed(1)}px)`;
      } else if (!isMobile) {
        // Reset quando superi la soglia su desktop
        heroSection.style.transform = 'translate3d(0, 0, 0) scale(1)';
        heroSection.style.opacity = '1';
        heroSection.style.filter = 'blur(0px)';
      } else {
        // SU MOBILE: Reset forzato per evitare scatti
        heroSection.style.transform = 'none';
        heroSection.style.opacity = '1';
        heroSection.style.filter = 'none';
      }
    }

    // 4.3 Sticky Scrollytelling Card Stack (Clean Layered Transitions without 3D Orbit)
    let activeIdx = 0;
    const isMobileDeck = window.innerWidth <= 768;
    if (scrollDeckSection && scrollCards.length > 0) {
      const rect = scrollDeckSection.getBoundingClientRect();
      const windowHeight = window.innerHeight;
      const totalScrollableHeight = rect.height - windowHeight;

      let progress = 0;
      if (totalScrollableHeight > 0) {
        progress = Math.max(0, Math.min(1, -rect.top / totalScrollableHeight));
      }

      const numCards = scrollCards.length;
      const exactCardPos = progress * (numCards - 1);
      activeIdx = Math.min(numCards - 1, Math.max(0, Math.round(exactCardPos)));

      scrollCards.forEach((card, i) => {
        const dist = i - exactCardPos;
        const absDist = Math.abs(dist);

        let translateY = 0;
        let scale = 1;
        let opacity = 1;
        let zIndex = 10;
        let pointerEvents = 'none';

        if (absDist <= 0.35) {
          // Card in absolute center view
          translateY = dist * 20;
          scale = 1 - absDist * 0.05;
          opacity = 1;
          zIndex = 100 - Math.round(absDist * 20);
          if (i === activeIdx) pointerEvents = 'auto';
        } else if (dist > 0.35) {
          // Cards waiting below: stacked cleanly with a slight downward shift and depth scale
          const stackDist = dist - 0.35;
          translateY = 7 + stackDist * 40;
          scale = Math.max(0.75, 0.98 - stackDist * 0.08);
          opacity = Math.max(0, 1 - stackDist * 1.2);
          zIndex = Math.max(1, 80 - Math.round(dist * 15));
        } else {
          // Cards scrolled past above: sliding smoothly upward out of view
          const pastDist = Math.abs(dist) - 0.35;
          translateY = -7 - pastDist * 80;
          scale = Math.max(0.75, 0.98 - pastDist * 0.08);
          opacity = Math.max(0, 1 - pastDist * 1.5);
          zIndex = Math.max(1, 80 - Math.round(Math.abs(dist) * 15));
        }

        card.style.visibility = opacity > 0.01 ? 'visible' : 'hidden';
        card.style.transform = `translate3d(0, ${translateY.toFixed(1)}px, 0) scale(${scale.toFixed(3)})`;
        card.style.opacity = opacity.toFixed(3);
        card.style.zIndex = `${zIndex}`;
        card.style.pointerEvents = pointerEvents;

        if (i === activeIdx) {
          card.style.boxShadow = '0 30px 70px rgba(0, 0, 0, 0.85), 0 0 35px rgba(45, 212, 191, 0.25), inset 0 1px 0 rgba(255, 255, 255, 0.2)';
        } else {
          card.style.boxShadow = '0 10px 30px rgba(0, 0, 0, 0.95)';
        }
      });
      // Effetto Simbionte a Scatto (Snapping Gooey Metaball & Elastic Stretch)
      const activeThumb = document.querySelector('.deck-sidebar-dots .deck-active-thumb');
      if (activeThumb && deckDots && deckDots.length > 0) {
        const targetDot = deckDots[activeIdx] || deckDots[0];
        if (targetDot) {
          const targetX = targetDot.offsetLeft + (targetDot.offsetWidth / 2);
          const targetY = targetDot.offsetTop + (targetDot.offsetHeight / 2);
          const baseWidth = isMobileDeck ? 18 : 14;
          const baseHeight = isMobileDeck ? 10 : 20;
          const thumbWidth = activeThumb.offsetWidth || baseWidth;
          const thumbHeight = activeThumb.offsetHeight || baseHeight;
          
          const leftPos = targetX - (baseWidth / 2);
          const topPos = targetY - (baseHeight / 2);

          // Controlla se activeIdx è cambiato rispetto al frame precedente per innescare lo scatto e l'allungamento organico
          if (activeThumb.dataset.lastIdx !== String(activeIdx)) {
            const oldIdx = parseInt(activeThumb.dataset.lastIdx || '0', 10);
            activeThumb.dataset.lastIdx = String(activeIdx);
            
            const distanceSteps = Math.abs(activeIdx - oldIdx) || 1;
            if (isMobileDeck) {
              // Allungamento elastico orizzontale per la barra pallini bottom (mobile)
              const stretchWidth = baseWidth + Math.min(28, distanceSteps * 12);
              const stretchScaleY = 0.65;
              const stretchLeftPos = targetX - (stretchWidth / 2);
              
              activeThumb.style.width = `${stretchWidth}px`;
              activeThumb.style.height = `${baseHeight}px`;
              activeThumb.style.transform = `translate3d(${stretchLeftPos.toFixed(1)}px, ${topPos.toFixed(1)}px, 0) scaleY(${stretchScaleY})`;
              
              clearTimeout(activeThumb.snapTimeout);
              activeThumb.snapTimeout = setTimeout(() => {
                activeThumb.style.width = `${baseWidth}px`;
                activeThumb.style.height = `${baseHeight}px`;
                activeThumb.style.transform = `translate3d(${leftPos.toFixed(1)}px, ${topPos.toFixed(1)}px, 0) scaleY(1)`;
              }, 180);
            } else {
              // Allungamento elastico verticale per la barra pallini laterale (desktop)
              const stretchHeight = baseHeight + Math.min(32, distanceSteps * 14);
              const stretchScaleX = 0.65;
              const stretchTopPos = targetY - (stretchHeight / 2);
              
              activeThumb.style.width = `${baseWidth}px`;
              activeThumb.style.height = `${stretchHeight}px`;
              activeThumb.style.transform = `translate3d(${leftPos.toFixed(1)}px, ${stretchTopPos.toFixed(1)}px, 0) scaleX(${stretchScaleX})`;
              
              clearTimeout(activeThumb.snapTimeout);
              activeThumb.snapTimeout = setTimeout(() => {
                activeThumb.style.width = `${baseWidth}px`;
                activeThumb.style.height = `${baseHeight}px`;
                activeThumb.style.transform = `translate3d(${leftPos.toFixed(1)}px, ${topPos.toFixed(1)}px, 0) scaleX(1)`;
              }, 180);
            }
          } else if (!activeThumb.snapTimeout) {
            // Mantiene la posizione saldamente bloccata sul pallino attivo (a scatto)
            activeThumb.style.width = `${baseWidth}px`;
            activeThumb.style.height = `${baseHeight}px`;
            activeThumb.style.transform = `translate3d(${leftPos.toFixed(1)}px, ${topPos.toFixed(1)}px, 0) scale(1, 1)`;
          }

          const colorClass = `color-${activeIdx % 3}`;
          if (!activeThumb.classList.contains(colorClass)) {
            activeThumb.classList.remove('color-0', 'color-1', 'color-2');
            activeThumb.classList.add(colorClass);
          }
        }
      }
    }

    requestAnimationFrame(animateScrollDynamics);
  };
  animateScrollDynamics();

  /* =========================================
     5. CINEMATIC PROJECTOR DUST & GOLDEN SPARKS ENGINE
     ========================================= */
  const canvas = document.getElementById('cyber-canvas');
  if (canvas) {
    const ctx = canvas.getContext('2d');
    let width = canvas.width = window.innerWidth;
    let height = canvas.height = window.innerHeight;

    window.addEventListener('resize', () => {
      width = canvas.width = window.innerWidth;
      height = canvas.height = window.innerHeight;
    }, { passive: true });

    const numParticles = Math.min(Math.floor((width * height) / 12000), 120);
    const particles = [];

    const colors = [
      'rgba(45, 212, 191, ',  // Teal
      'rgba(96, 239, 255, ',  // Cyan Glow
      'rgba(255, 255, 255, ', // Silver Dust
      'rgba(10, 174, 156, '   // Deep Teal
    ];

    for (let i = 0; i < numParticles; i++) {
      particles.push({
        x: Math.random() * width,
        y: Math.random() * height,
        radius: Math.random() * 2.2 + 0.5,
        colorBase: colors[Math.floor(Math.random() * colors.length)],
        alpha: Math.random() * 0.7 + 0.1,
        speedX: (Math.random() - 0.5) * 0.4,
        speedY: (Math.random() * -0.6) - 0.2, // Drift gently upward like dust in projector light
        wobble: Math.random() * Math.PI * 2,
        wobbleSpeed: (Math.random() - 0.5) * 0.03
      });
    }

    let mouseCanvasX = width / 2;
    let mouseCanvasY = height / 2;
    window.addEventListener('mousemove', (e) => {
      mouseCanvasX = e.clientX;
      mouseCanvasY = e.clientY;
    }, { passive: true });

    const renderCinemaDust = () => {
      ctx.clearRect(0, 0, width, height);

      // Draw subtle projector light cone bloom on canvas
      const grad = ctx.createRadialGradient(width / 2, 0, 10, width / 2, height * 0.5, width * 0.6);
      grad.addColorStop(0, 'rgba(45, 212, 191, 0.05)');
      grad.addColorStop(1, 'transparent');
      ctx.fillStyle = grad;
      ctx.fillRect(0, 0, width, height);

      particles.forEach(p => {
        p.wobble += p.wobbleSpeed;
        p.x += p.speedX + Math.sin(p.wobble) * 0.3;
        p.y += p.speedY;

        // Interactive mouse dispersion
        const dx = p.x - mouseCanvasX;
        const dy = p.y - mouseCanvasY;
        const dist = Math.sqrt(dx * dx + dy * dy);
        if (dist < 120) {
          const force = (120 - dist) / 120;
          p.x += (dx / dist) * force * 1.5;
          p.y += (dy / dist) * force * 1.5;
        }

        // Wrap around screen
        if (p.y < -10) p.y = height + 10;
        if (p.x < -10) p.x = width + 10;
        if (p.x > width + 10) p.x = -10;

        // Twinkle effect
        const currentAlpha = Math.min(1, Math.max(0.05, p.alpha + Math.sin(p.wobble * 3) * 0.2));

        ctx.beginPath();
        ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
        ctx.fillStyle = `${p.colorBase}${currentAlpha.toFixed(2)})`;
        ctx.shadowBlur = 8;
        ctx.shadowColor = `${p.colorBase}0.8)`;
        ctx.fill();
        ctx.shadowBlur = 0; // reset
      });

      requestAnimationFrame(renderCinemaDust);
    };
    renderCinemaDust();
  }

});


/* =========================================
     EFFETTO PULSANTI MAGNETICI
     ========================================= */
const magneticBtns = document.querySelectorAll('.magnetic-btn');

magneticBtns.forEach(btn => {
  btn.addEventListener('mousemove', (e) => {
    const rect = btn.getBoundingClientRect();
    // Calcola la distanza del mouse dal centro del pulsante
    const x = (e.clientX - rect.left) - rect.width / 2;
    const y = (e.clientY - rect.top) - rect.height / 2;

    // Muovi il pulsante di una frazione della distanza (forza magnetica)
    btn.style.transform = `translate(${x * 0.3}px, ${y * 0.3}px) scale(1.05)`;
  });

  btn.addEventListener('mouseleave', () => {
    // Quando il mouse esce, rimuovi il transform per far agire la transizione CSS
    btn.style.transform = '';
  });
});

/* =========================================
     EFFETTO SPOTLIGHT SUI PULSANTI
     ========================================= */
const spotlightBtns = document.querySelectorAll('.btn-primary, .btn-secondary');

spotlightBtns.forEach(btn => {
  btn.addEventListener('mousemove', (e) => {
    const rect = btn.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    // Passa le coordinate esatte al CSS
    btn.style.setProperty('--x', `${x}px`);
    btn.style.setProperty('--y', `${y}px`);
  });
});


/* =========================================
     EFFETTO PROFONDITÀ (PANNING INTERNO)
     ========================================= */
const panImages = document.querySelectorAll('.screenshot-item');

panImages.forEach(container => {
  const img = container.querySelector('img');

  // Assicuriamoci che l'immagine sia un po' più grande del contenitore per poterla muovere
  if (img) {
    // Ingrandiamo col transform invece che con la larghezza per mantenere il centro perfetto
    img.style.transformOrigin = "center center";
    img.style.transition = "transform 0.1s linear";
    img.style.transform = "scale(1.05)";
  }

  container.addEventListener('mousemove', (e) => {
    if (!img) return;
    const rect = container.getBoundingClientRect();

    // Calcola la posizione percentuale del mouse (da -0.5 a +0.5)
    const xPos = ((e.clientX - rect.left) / rect.width) - 0.5;
    const yPos = ((e.clientY - rect.top) / rect.height) - 0.5;

    // Muovi l'immagine in direzione OPPOSTA al mouse (effetto parallasse interno)
    const xOffset = xPos * -15; // px di spostamento massimo
    const yOffset = yPos * -15;

    img.style.transform = `translate(${xOffset}px, ${yOffset}px) scale(1.05)`;
  });

  container.addEventListener('mouseleave', () => {
    if (img) {
      img.style.transition = "transform 0.5s cubic-bezier(0.16, 1, 0.3, 1)";
      img.style.transform = `translate(0px, 0px) scale(1)`; // Torna alla dimensione normale
    }
  });

  // Rimuovi la transizione lenta quando rientri per un tracciamento immediato
  container.addEventListener('mouseenter', () => {
    if (img) img.style.transition = "none";
  });
});

/* =========================================
     HOLOGRAPHIC LIGHTBOX & ZOOM ENGINE
     ========================================= */
const lightbox = document.getElementById('screenshot-modal') || document.querySelector('.lightbox-modal');
const lightboxImg = lightbox ? (lightbox.querySelector('#lightbox-img') || lightbox.querySelector('img')) : null;
const lightboxClose = lightbox ? lightbox.querySelector('.lightbox-close') : null;
const lightboxCaption = lightbox ? lightbox.querySelector('#lightbox-caption') : null;
const lightboxWrapper = lightbox ? lightbox.querySelector('.lightbox-img-wrapper') : null;

let isZoomed = false;

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
  if (lightbox) lightbox.classList.remove('active');
  document.body.style.overflow = '';

  // Reset automatico dello zoom alla chiusura
  isZoomed = false;
  if (lightboxImg) {
    lightboxImg.style.transform = 'scale(1)';
    lightboxImg.style.cursor = 'zoom-in';
    setTimeout(() => {
      if (!isZoomed) lightboxImg.style.transformOrigin = 'center center';
    }, 300);
  }
};

// Apertura al click sugli screenshot
document.querySelectorAll('.screenshot-item').forEach(item => {
  item.addEventListener('click', () => {
    const img = item.querySelector('img');
    const captionEl = item.querySelector('.screenshot-caption span');
    const captionText = captionEl ? captionEl.textContent : (img ? img.alt : '');
    if (img) openLightbox(img.src, img.alt, captionText);
  });
});

// Chiusura dai vari trigger
if (lightbox) {
  if (lightboxClose) lightboxClose.addEventListener('click', closeLightbox);
  const lightboxBackdrop = lightbox.querySelector('.lightbox-backdrop');
  if (lightboxBackdrop) lightboxBackdrop.addEventListener('click', closeLightbox);

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && lightbox.classList.contains('active')) closeLightbox();
  });
}

// MOTORE DI ZOOM E PANNING
if (lightboxWrapper && lightboxImg) {
  lightboxImg.style.cursor = 'zoom-in';

  const updatePan = (e) => {
    const rect = lightboxWrapper.getBoundingClientRect();
    const x = ((e.clientX - rect.left) / rect.width) * 100;
    const y = ((e.clientY - rect.top) / rect.height) * 100;
    lightboxImg.style.transformOrigin = `${x}% ${y}%`;
  };

  lightboxWrapper.addEventListener('click', (e) => {
    // Ignoriamo il click se l'utente clicca sul bottone di chiusura
    if (e.target.classList.contains('lightbox-close')) return;

    isZoomed = !isZoomed;
    if (isZoomed) {
      lightboxImg.style.cursor = 'zoom-out';
      lightboxImg.style.transform = 'scale(2.4)'; // Zoom del 240%
      updatePan(e);
    } else {
      lightboxImg.style.cursor = 'zoom-in';
      lightboxImg.style.transform = 'scale(1)';
    }
  });

  lightboxWrapper.addEventListener('mousemove', (e) => {
    if (isZoomed) updatePan(e);
  });

  lightboxWrapper.addEventListener('mouseleave', () => {
    if (isZoomed) {
      isZoomed = false;
      lightboxImg.style.cursor = 'zoom-in';
      lightboxImg.style.transform = 'scale(1)';
    }
  });
}

/* Magnetismo leggero per i link del footer */
const footerLinks = document.querySelectorAll('.footer-links a');
footerLinks.forEach(link => {
  link.addEventListener('mousemove', (e) => {
    const rect = link.getBoundingClientRect();
    const x = (e.clientX - rect.left - rect.width / 2) * 0.2;
    const y = (e.clientY - rect.top - rect.height / 2) * 0.2;
    link.style.transform = `translate(${x}px, ${y}px)`;
    link.style.color = "var(--accent-teal)";
  });
  link.addEventListener('mouseleave', () => {
    link.style.transform = '';
    link.style.color = '';
  });
});

/* =========================================
   INTERACTIVE CINEMA DUST CANVAS ENGINE
   ========================================= */
const initCinemaDustCanvas = () => {
  if (window.__cinemaDustInitialized) return;
  const canvas = document.getElementById('cyber-canvas');
  if (!canvas) return;
  window.__cinemaDustInitialized = true;

  // Forza stili fissi ad altissima priorità nel caso il CSS venga coperto
  canvas.style.position = 'fixed';
  canvas.style.top = '0';
  canvas.style.left = '0';
  canvas.style.width = '100vw';
  canvas.style.height = '100vh';
  canvas.style.pointerEvents = 'none';
  canvas.style.zIndex = '999';

  const ctx = canvas.getContext('2d');
  let width, height;
  let particles = [];
  const particleCount = 45; // Effetto minimal, discreto e ultra-elegante

  let mouse = { x: -1000, y: -1000, vx: 0, vy: 0 };
  let lastMouse = { x: -1000, y: -1000 };

  window.addEventListener('mousemove', (e) => {
    mouse.vx = (e.clientX - lastMouse.x) * 0.03;
    mouse.vy = (e.clientY - lastMouse.y) * 0.03;
    mouse.x = e.clientX;
    mouse.y = e.clientY;
    lastMouse.x = e.clientX;
    lastMouse.y = e.clientY;
  });

  const resizeCanvas = () => {
    width = window.innerWidth || document.documentElement.clientWidth;
    height = window.innerHeight || document.documentElement.clientHeight;
    canvas.width = width;
    canvas.height = height;
  };

  class DustParticle {
    constructor() {
      this.reset(true);
    }

    reset(initial = false) {
      this.x = Math.random() * width;
      this.y = initial ? Math.random() * height : height + 15;
      // Micro-granelli leggeri e raffinati
      this.size = Math.random() * 1.1 + 0.7;
      // Movimento calmissimo e ipnotico (quasi sospeso nell'aria)
      this.baseSpeedY = -(Math.random() * 0.12 + 0.04);
      this.speedX = (Math.random() - 0.5) * 0.07;
      this.speedY = this.baseSpeedY;

      const palette = [
        'rgba(245, 245, 250, ',
        'rgba(255, 248, 220, ',
        'rgba(200, 245, 235, '
      ];
      this.colorPrefix = palette[Math.floor(Math.random() * palette.length)];
      // Opacità elegante e morbida
      this.baseAlpha = Math.random() * 0.15 + 0.10;
      this.alpha = this.baseAlpha;
      // Sfarfallio lentissimo come un respiro placido
      this.twinkleSpeed = Math.random() * 0.006 + 0.003;
      this.twinkleAngle = Math.random() * Math.PI * 2;
    }

    update() {
      this.twinkleAngle += this.twinkleSpeed;
      this.alpha = this.baseAlpha + Math.sin(this.twinkleAngle) * 0.04;
      if (this.alpha < 0.06) this.alpha = 0.06;

      const dx = this.x - mouse.x;
      const dy = this.y - mouse.y;
      const distSq = dx * dx + dy * dy;
      const maxDist = 130;

      if (distSq < maxDist * maxDist) {
        const dist = Math.sqrt(distSq) || 1;
        const force = (maxDist - dist) / maxDist;
        // Reazione dolcissima come una brezza leggerissima
        this.x += (dx / dist) * force * 0.35 + mouse.vx * force * 0.15;
        this.y += (dy / dist) * force * 0.35 + mouse.vy * force * 0.15;
      }

      this.x += this.speedX;
      this.y += this.speedY;

      this.speedY += (this.baseSpeedY - this.speedY) * 0.03;

      if (this.y < -20 || this.x < -20 || this.x > width + 20) {
        this.reset(false);
      }
    }

    draw() {
      const safeAlpha = Math.min(0.25, Math.max(0.05, this.alpha));
      // Granello ultra-fine
      ctx.beginPath();
      ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
      ctx.fillStyle = `${this.colorPrefix}${safeAlpha.toFixed(3)})`;
      ctx.fill();
    }
  }

  resizeCanvas();
  window.addEventListener('resize', resizeCanvas);

  particles = Array.from({ length: particleCount }, () => new DustParticle());

  const animate = () => {
    ctx.clearRect(0, 0, width, height);
    particles.forEach((p) => {
      p.update();
      p.draw();
    });
    mouse.vx *= 0.9;
    mouse.vy *= 0.9;
    requestAnimationFrame(animate);
  };

  animate();
};

if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initCinemaDustCanvas);
} else {
  initCinemaDustCanvas();
}

/* ==========================================================================
   NEXT-GEN FLOW CONSTRUCTION ENGINE (Lenis, Word Reveal, Flow Assembly, Magnetic LERP)
   ========================================================================== */
document.addEventListener('DOMContentLoaded', () => {
  // 1. Lenis Smooth Scrolling Engine (120 FPS Momentum Inertia)
  if (typeof Lenis !== 'undefined') {
    const lenis = new Lenis({
      duration: 1.25,
      easing: (t) => Math.min(1, 1.001 - Math.pow(2, -10 * t)),
      smoothWheel: true,
      wheelMultiplier: 1.0
    });
    function raf(time) {
      lenis.raf(time);
      requestAnimationFrame(raf);
    }
    requestAnimationFrame(raf);
    window.lenis = lenis;
  }

  // 2. Split-Text Blur-to-Clear Word Reveal Engine
  const initBlurWordReveal = () => {
    const revealTargets = document.querySelectorAll('.blur-word-reveal, .hero h1, .section-header h2, .section-header p');
    revealTargets.forEach(el => {
      if (el.dataset.wordSplit) return;
      el.dataset.wordSplit = "true";
      const htmlText = el.innerHTML;
      if (htmlText.includes('<')) {
        const childNodes = Array.from(el.childNodes);
        el.innerHTML = '';
        childNodes.forEach(node => {
          if (node.nodeType === Node.TEXT_NODE) {
            const words = node.textContent.split(/\s+/).filter(w => w.length > 0);
            words.forEach((word) => {
              const span = document.createElement('span');
              span.className = 'blur-word';
              span.textContent = word;
              el.appendChild(span);
            });
          } else if (node.nodeType === Node.ELEMENT_NODE) {
            if (node.tagName.toLowerCase() === 'br') {
              el.appendChild(node.cloneNode());
            } else {
              const innerWords = node.textContent.split(/\s+/).filter(w => w.length > 0);
              const clone = node.cloneNode(false);
              clone.innerHTML = innerWords.map(w => `<span class="blur-word">${w}</span>`).join('');
              el.appendChild(clone);
            }
          }
        });
      } else {
        const words = el.textContent.split(/\s+/).filter(w => w.length > 0);
        el.innerHTML = words.map(w => `<span class="blur-word">${w}</span>`).join('');
      }
    });

    const observer = new IntersectionObserver((entries, obs) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          const blurWords = entry.target.querySelectorAll('.blur-word');
          blurWords.forEach((wordSpan, idx) => {
            setTimeout(() => {
              wordSpan.classList.add('revealed');
            }, idx * 45);
          });
          obs.unobserve(entry.target);
        }
      });
    }, { threshold: 0.15, rootMargin: '0px 0px -40px 0px' });

    revealTargets.forEach(el => observer.observe(el));
  };
  initBlurWordReveal();

  // 3. Flow Construct SVG Icon & Pipeline Assembly Engine
  const initFlowAssemblyEngine = () => {
    const flowContainers = document.querySelectorAll('.flow-pipeline-section, .flow-node-card, .deck-scroll-card, .feature-card');
    const flowObserver = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('flow-active');
        }
      });
    }, { threshold: 0.2 });

    flowContainers.forEach(container => flowObserver.observe(container));

    // Interactive Hover Acceleration on Pipeline Nodes
    const pipelineContainer = document.querySelector('.pipeline-container');
    const energyPackets = document.querySelectorAll('.energy-packet');
    if (pipelineContainer && energyPackets.length > 0) {
      pipelineContainer.addEventListener('mouseenter', () => {
        energyPackets.forEach(p => p.style.animationDuration = '1.6s');
      });
      pipelineContainer.addEventListener('mouseleave', () => {
        energyPackets.forEach((p, idx) => {
          p.style.animationDuration = idx === 0 ? '3.5s' : idx === 1 ? '4s' : '3.2s';
        });
      });
    }
  };
  initFlowAssemblyEngine();

  // 4. Spring Physics Magnetic Button LERP
  const initSpringMagneticButtons = () => {
    const magneticBtns = document.querySelectorAll('.magnetic-btn, .btn-primary, .btn-secondary');
    magneticBtns.forEach(btn => {
      let x = 0, y = 0, targetX = 0, targetY = 0;
      let isHovered = false;

      btn.addEventListener('mousemove', (e) => {
        const rect = btn.getBoundingClientRect();
        const centerX = rect.left + rect.width / 2;
        const centerY = rect.top + rect.height / 2;
        targetX = (e.clientX - centerX) * 0.35;
        targetY = (e.clientY - centerY) * 0.35;
        isHovered = true;
      });

      btn.addEventListener('mouseleave', () => {
        targetX = 0;
        targetY = 0;
        isHovered = false;
      });

      const animateSpring = () => {
        x += (targetX - x) * 0.12;
        y += (targetY - y) * 0.12;
        if (Math.abs(x) > 0.01 || Math.abs(y) > 0.01 || isHovered) {
          btn.style.transform = `translate3d(${x.toFixed(2)}px, ${y.toFixed(2)}px, 0)`;
        } else {
          btn.style.transform = '';
        }
        requestAnimationFrame(animateSpring);
      };
      animateSpring();
    });
  };
  initSpringMagneticButtons();

  // 5. Holographic Stage 3D Circular Orbit Carousel & Dynamic Color Extraction Engine
  const initHolographicCoverStage = () => {
    const coverCards = Array.from(document.querySelectorAll('.holographic-cover-card'));
    const stageBg = document.getElementById('stage-ambient-bg');
    const stageContainer = document.querySelector('.holographic-stage-container');
    if (!coverCards.length || !stageBg || !stageContainer) return;

    let stageAngle = 0;
    let isStageHovered = false;
    let stageSpeed = 1.0;
    let lastStageTs = performance.now();

    stageContainer.addEventListener('mouseenter', () => { isStageHovered = true; });
    stageContainer.addEventListener('mouseleave', () => { isStageHovered = false; });
    stageContainer.addEventListener('touchstart', () => { isStageHovered = true; }, { passive: true });
    stageContainer.addEventListener('touchend', () => {
      setTimeout(() => { isStageHovered = false; }, 3000);
    }, { passive: true });

    coverCards.forEach((card, idx) => {
      const color1 = card.getAttribute('data-color-1') || '#2DD4BF';
      const color2 = card.getAttribute('data-color-2') || '#BB86FC';
      const color3 = card.getAttribute('data-color-3') || color1;
      card.style.setProperty('--card-glow-1', `${color1}AA`);
      card.style.setProperty('--card-glow-2', `${color2}AA`);
      card.style.setProperty('--card-glow-3', `${color3}AA`);

      card.addEventListener('mouseenter', () => {
        isStageHovered = true;
        stageBg.style.setProperty('--stage-glow-color', color1);
      });

      card.addEventListener('mouseleave', () => {
        isStageHovered = false;
        stageBg.style.setProperty('--stage-glow-color', 'rgba(45, 212, 191, 0.18)');
      });

      card.addEventListener('click', () => {
        // Bring clicked card directly to front (0 degrees)
        const targetCardAngle = (idx * 360 / coverCards.length);
        let diff = (360 - targetCardAngle) - stageAngle;
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;
        stageAngle = (stageAngle + diff + 3600) % 360;
      });
    });

    const animateStage3D = () => {
      const nowTs = performance.now();
      let deltaSec = (nowTs - lastStageTs) / 1000;
      lastStageTs = nowTs;
      if (deltaSec > 0.1 || deltaSec < 0) deltaSec = 0.016;

      const targetSpeed = isStageHovered ? 0.08 : 1.0;
      stageSpeed += (targetSpeed - stageSpeed) * Math.min(1, deltaSec * 6);
      stageAngle = (stageAngle + (15 * stageSpeed * deltaSec)) % 360;

      const numCards = coverCards.length;
      const isMobile = window.innerWidth <= 768;

      const rect = stageContainer.getBoundingClientRect();
      const stageCenterY = rect.top + rect.height / 2;
      const windowCenterY = window.innerHeight / 2;
      const verticalOffsetNorm = (windowCenterY - stageCenterY) / window.innerHeight;
      const scrollRotateX = Math.max(-35, Math.min(35, verticalOffsetNorm * -45));

      coverCards.forEach((card, i) => {
        let angleDeg = (stageAngle + (i * 360 / numCards)) % 360;
        if (angleDeg > 180) angleDeg -= 360;
        if (angleDeg < -180) angleDeg += 360;

        const rad = angleDeg * Math.PI / 180;
        const cosVal = Math.cos(rad); // 1 (front) to -1 (back)
        const sinVal = Math.sin(rad); // 0 (front) -> 1 (right) -> 0 (back) -> -1 (left)

        // depthNorm smoothly ranges from 1.0 (in front) to 0.0 (in back)
        const depthNorm = (cosVal + 1) / 2;

        const radiusX = isMobile ? 160 : 340;
        const radiusZ = isMobile ? 200 : 350;

        const translateX = sinVal * radiusX;
        const translateZ = cosVal * radiusZ;
        const translateY = (1 - cosVal) * (isMobile ? -8 : -16);

        const rotateY = sinVal * (isMobile ? 18 : 34);
        const rotateX = scrollRotateX + (1 - cosVal) * 5;

        const scale = isMobile
          ? (0.70 + depthNorm * 0.30)
          : (0.65 + depthNorm * 0.38);

        const opacity = isMobile
          ? Math.max(0.25, (0.3 + depthNorm * 0.7))
          : Math.max(0.2, (0.25 + depthNorm * 0.75));

        const zIndex = Math.round(depthNorm * 100) + 10;

        card.style.transform = `translate3d(${translateX.toFixed(1)}px, ${translateY.toFixed(1)}px, ${translateZ.toFixed(1)}px) rotateY(${rotateY.toFixed(1)}deg) rotateX(${rotateX.toFixed(1)}deg) scale(${scale.toFixed(3)})`;
        card.style.opacity = opacity.toFixed(3);
        card.style.zIndex = `${zIndex}`;
        card.style.pointerEvents = depthNorm > 0.65 ? 'auto' : 'none';

        if (depthNorm > 0.9) {
          card.classList.add('is-front');
        } else {
          card.classList.remove('is-front');
        }
      });

      requestAnimationFrame(animateStage3D);
    };

    requestAnimationFrame(animateStage3D);
  };

  // ==========================================
  // DYNAMIC EXACT SYNAPSE BEAMS CONNECTION
  // ==========================================
  const initCyberMigrationSynapses = () => {
    const stage = document.getElementById('synapse-stage-container');
    const svg = document.getElementById('electric-beams-svg');
    const reactor = document.getElementById('flicktrove-core-reactor');
    if (!stage || !svg || !reactor) return;

    const sockets = [
      { id: 'tvtime', socket: document.getElementById('socket-tvtime'), side: 'left' },
      { id: 'trakt', socket: document.getElementById('socket-trakt'), side: 'left' },
      { id: 'letterboxd', socket: document.getElementById('socket-letterboxd'), side: 'right' },
      { id: 'imdb', socket: document.getElementById('socket-imdb'), side: 'right' }
    ];

    const updateBeams = () => {
      const stageRect = stage.getBoundingClientRect();
      if (stageRect.width === 0 || stageRect.height === 0) return;

      svg.setAttribute('viewBox', `0 0 ${stageRect.width} ${stageRect.height}`);

      const reactorRect = reactor.getBoundingClientRect();
      const reactorCenterX = reactorRect.left + reactorRect.width / 2 - stageRect.left;
      const reactorCenterY = reactorRect.top + reactorRect.height / 2 - stageRect.top;
      const reactorRadius = reactorRect.width / 2 - 4; // connect right to the outer ring edge

      sockets.forEach(item => {
        if (!item.socket) return;
        const sockRect = item.socket.getBoundingClientRect();
        const startX = sockRect.left + sockRect.width / 2 - stageRect.left;
        const startY = sockRect.top + sockRect.height / 2 - stageRect.top;

        // Calculate exact point on the reactor ring circumference towards the socket
        const dx = startX - reactorCenterX;
        const dy = startY - reactorCenterY;
        const angle = Math.atan2(dy, dx);
        const endX = reactorCenterX + Math.cos(angle) * reactorRadius;
        const endY = reactorCenterY + Math.sin(angle) * reactorRadius;

        // Cubic Bezier control points for smooth S-curve (horizontal on desktop, vertical on mobile)
        const isMobileSynapse = window.innerWidth <= 850 || stageRect.width <= 850;
        let c1x, c1y, c2x, c2y;
        if (isMobileSynapse) {
          const controlOffsetY = Math.abs(endY - startY) * 0.45;
          c1x = startX;
          c1y = startY < endY ? startY + controlOffsetY : startY - controlOffsetY;
          c2x = endX;
          c2y = startY < endY ? endY - controlOffsetY : endY + controlOffsetY;
        } else {
          const controlOffsetX = Math.abs(endX - startX) * 0.45;
          c1x = item.side === 'left' ? startX + controlOffsetX : startX - controlOffsetX;
          c1y = startY;
          c2x = item.side === 'left' ? endX - controlOffsetX : endX + controlOffsetX;
          c2y = endY;
        }

        const pathData = `M ${startX.toFixed(1)} ${startY.toFixed(1)} C ${c1x.toFixed(1)} ${c1y.toFixed(1)}, ${c2x.toFixed(1)} ${c2y.toFixed(1)}, ${endX.toFixed(1)} ${endY.toFixed(1)}`;

        const pathEl = document.getElementById(`line-${item.id}`);
        const motionEl = document.getElementById(`bolt-motion-${item.id}`);
        if (pathEl) pathEl.setAttribute('d', pathData);
        if (motionEl) motionEl.setAttribute('path', pathData);
      });
    };

    updateBeams();
    window.addEventListener('resize', updateBeams);
    setTimeout(updateBeams, 100);
    setTimeout(updateBeams, 500);
    setTimeout(updateBeams, 1500);
  };
  initCyberMigrationSynapses();

  initHolographicCoverStage();
});