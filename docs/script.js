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
        mobileBtn.innerHTML = '☰';
        document.documentElement.classList.remove('drawer-open');
        document.body.classList.remove('drawer-open');
        document.documentElement.style.overflow = '';
        document.body.style.overflow = '';
      } else {
        mobileDrawer.classList.add('active');
        mobileBtn.innerHTML = '✕';
        document.documentElement.classList.add('drawer-open');
        document.body.classList.add('drawer-open');
        document.documentElement.style.overflow = 'hidden';
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
      'visual perfection.',
      'TMDB and Trakt sync.',
      '100% offline use.',
      'finding where to stream.',
      'you.'
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
  const filmstripHoles = document.querySelectorAll('.filmstrip-divider .film-holes');
  const filmstripTracks = document.querySelectorAll('.filmstrip-divider .film-line');

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

    // 4.1.5 Cinematic Filmstrip Horizontal Scroll
    if (filmstripHoles.length > 0) {
      const scrollPos = window.scrollY * 0.55;
      filmstripHoles.forEach((hole) => {
        hole.style.backgroundPosition = `${scrollPos.toFixed(1)}px 0px`;
      });
      filmstripTracks.forEach((track) => {
        track.style.backgroundPosition = `${scrollPos.toFixed(1)}px 0px`;
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