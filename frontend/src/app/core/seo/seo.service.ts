import { DOCUMENT } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';

export interface DatosSeo {
  titulo: string;
  descripcion: string;
  palabrasClave?: string | null;
  urlCanonica?: string | null;
  imagen?: string | null;
  jsonLd?: Record<string, unknown> | null;
}

/**
 * Metadatos de cada página pública (RF05, RNF07). Se escriben durante el SSR, así que los
 * buscadores los reciben en el HTML inicial.
 */
@Injectable({ providedIn: 'root' })
export class SeoService {
  private readonly titulo = inject(Title);
  private readonly meta = inject(Meta);
  private readonly documento = inject(DOCUMENT);

  aplicar(datos: DatosSeo): void {
    this.titulo.setTitle(datos.titulo);
    this.meta.updateTag({ name: 'description', content: datos.descripcion });
    this.etiqueta('keywords', datos.palabrasClave);
    this.meta.updateTag({ property: 'og:title', content: datos.titulo });
    this.meta.updateTag({ property: 'og:description', content: datos.descripcion });
    this.meta.updateTag({ property: 'og:type', content: datos.jsonLd?.['@type'] === 'Product' ? 'product' : 'website' });
    this.propiedad('og:url', datos.urlCanonica);
    this.propiedad('og:image', datos.imagen);
    this.canonica(datos.urlCanonica);
    this.jsonLd(datos.jsonLd);
  }

  private etiqueta(nombre: string, valor?: string | null): void {
    if (valor) this.meta.updateTag({ name: nombre, content: valor });
    else this.meta.removeTag(`name="${nombre}"`);
  }

  private propiedad(nombre: string, valor?: string | null): void {
    if (valor) this.meta.updateTag({ property: nombre, content: valor });
    else this.meta.removeTag(`property="${nombre}"`);
  }

  private canonica(url?: string | null): void {
    let enlace = this.documento.head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (!url) {
      enlace?.remove();
      return;
    }
    if (!enlace) {
      enlace = this.documento.createElement('link');
      enlace.setAttribute('rel', 'canonical');
      this.documento.head.appendChild(enlace);
    }
    enlace.setAttribute('href', url);
  }

  private jsonLd(datos?: Record<string, unknown> | null): void {
    this.documento.head.querySelector('script[data-nalu-jsonld]')?.remove();
    if (!datos) return;
    const script = this.documento.createElement('script');
    script.setAttribute('type', 'application/ld+json');
    script.setAttribute('data-nalu-jsonld', '');
    // "<" escapado: un texto con "</script>" no puede cerrar la etiqueta (XSS).
    script.textContent = JSON.stringify(datos).replace(/</g, '\\u003c');
    this.documento.head.appendChild(script);
  }
}
