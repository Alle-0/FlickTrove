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
});
