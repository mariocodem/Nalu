import { registerLocaleData } from '@angular/common';
import localeEsCo from '@angular/common/locales/es-CO';
import {
  ApplicationConfig,
  LOCALE_ID,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideClientHydration } from '@angular/platform-browser';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { urlApiInterceptor } from './core/api/url-api';
import { AuthService } from './core/auth/auth.service';

registerLocaleData(localeEsCo);

export const appConfig: ApplicationConfig = {
  providers: [
    { provide: LOCALE_ID, useValue: 'es-CO' },
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideClientHydration(),
    provideHttpClient(withFetch(), withInterceptors([urlApiInterceptor, authInterceptor])),
    // Recupera la sesión al arrancar en el navegador; en SSR no hace ninguna petición.
    provideAppInitializer(() => inject(AuthService).restaurarSesion()),
  ],
};
