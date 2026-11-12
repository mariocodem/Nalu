import { mergeApplicationConfig, ApplicationConfig } from '@angular/core';
import { provideServerRendering, withRoutes } from '@angular/ssr';
import { appConfig } from './app.config';
import { serverRoutes } from './app.routes.server';
import { URL_API_SERVIDOR } from './core/api/url-api';

const serverConfig: ApplicationConfig = {
  providers: [
    provideServerRendering(withRoutes(serverRoutes)),
    // Dónde está la API vista desde el servidor SSR (variable NALU_API_URL).
    { provide: URL_API_SERVIDOR, useValue: process.env['NALU_API_URL'] ?? 'http://localhost:8080' },
  ]
};

export const config = mergeApplicationConfig(appConfig, serverConfig);
