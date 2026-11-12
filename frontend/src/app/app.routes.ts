import { Routes } from '@angular/router';
import { rolGuard } from './core/auth/rol.guard';

export const routes: Routes = [
  { path: '', title: 'Nalu · Joyería', loadComponent: () => import('./tienda/inicio/inicio').then((m) => m.Inicio) },
  {
    path: 'catalogo',
    loadComponent: () => import('./tienda/catalogo/catalogo').then((m) => m.Catalogo),
  },
  {
    path: 'catalogo/:categoria',
    loadComponent: () => import('./tienda/catalogo/catalogo').then((m) => m.Catalogo),
  },
  {
    path: 'producto/:slug',
    loadComponent: () => import('./tienda/producto/ficha-producto').then((m) => m.FichaProductoPagina),
  },
  { path: 'bolsa', title: 'Tu bolsa · Nalu', loadComponent: () => import('./tienda/bolsa/bolsa').then((m) => m.Bolsa) },
  {
    path: 'checkout',
    title: 'Finalizar compra · Nalu',
    canActivate: [rolGuard('CLIENTE')],
    loadComponent: () => import('./tienda/checkout/checkout').then((m) => m.Checkout),
  },
  {
    path: 'pago/resultado',
    title: 'Resultado del pago · Nalu',
    canActivate: [rolGuard('CLIENTE')],
    loadComponent: () => import('./tienda/pago/resultado-pago').then((m) => m.ResultadoPago),
  },
  {
    path: 'cuenta',
    children: [
      {
        path: 'pedidos',
        title: 'Mis pedidos · Nalu',
        canActivate: [rolGuard('CLIENTE')],
        loadComponent: () => import('./cuenta/pedidos/mis-pedidos').then((m) => m.MisPedidos),
      },
      {
        path: 'pedidos/:numero',
        title: 'Pedido · Nalu',
        canActivate: [rolGuard('CLIENTE')],
        loadComponent: () => import('./cuenta/pedidos/detalle-pedido').then((m) => m.DetallePedido),
      },
      { path: 'login', title: 'Ingresar · Nalu', loadComponent: () => import('./cuenta/login/login').then((m) => m.Login) },
      {
        path: 'registro',
        title: 'Crear cuenta · Nalu',
        loadComponent: () => import('./cuenta/registro/registro').then((m) => m.Registro),
      },
      {
        path: '',
        title: 'Mi cuenta · Nalu',
        canActivate: [rolGuard('CLIENTE')],
        loadComponent: () => import('./cuenta/perfil/mi-cuenta').then((m) => m.MiCuenta),
      },
    ],
  },
  {
    path: 'admin',
    canActivate: [rolGuard('ADMINISTRADOR')],
    loadComponent: () => import('./admin/admin-layout').then((m) => m.AdminLayout),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'pedidos' },
      {
        path: 'productos',
        title: 'Productos · Panel Nalu',
        loadComponent: () => import('./admin/productos/lista-productos').then((m) => m.ListaProductos),
      },
      {
        path: 'productos/nuevo',
        title: 'Nuevo producto · Panel Nalu',
        loadComponent: () => import('./admin/productos/editor-producto').then((m) => m.EditorProducto),
      },
      {
        path: 'productos/:id',
        title: 'Producto · Panel Nalu',
        loadComponent: () => import('./admin/productos/editor-producto').then((m) => m.EditorProducto),
      },
      {
        path: 'categorias',
        title: 'Categorías · Panel Nalu',
        loadComponent: () => import('./admin/categorias/categorias').then((m) => m.Categorias),
      },
      {
        path: 'pedidos',
        title: 'Pedidos · Panel Nalu',
        loadComponent: () => import('./admin/pedidos/pedidos-admin').then((m) => m.PedidosAdmin),
      },
      {
        path: 'pedidos/:numero',
        title: 'Pedido · Panel Nalu',
        loadComponent: () => import('./admin/pedidos/pedido-admin').then((m) => m.PedidoAdmin),
      },
      {
        path: 'inventario',
        title: 'Inventario · Panel Nalu',
        loadComponent: () => import('./admin/inventario/inventario').then((m) => m.Inventario),
      },
      {
        path: 'resenas',
        title: 'Reseñas · Panel Nalu',
        loadComponent: () => import('./admin/resenas/resenas-admin').then((m) => m.ResenasAdmin),
      },
      {
        path: 'reportes',
        title: 'Reportes · Panel Nalu',
        loadComponent: () => import('./admin/reportes/reportes').then((m) => m.Reportes),
      },
      {
        path: 'auditoria',
        title: 'Auditoría · Panel Nalu',
        loadComponent: () => import('./admin/auditoria/auditoria').then((m) => m.Auditoria),
      },
      {
        path: 'promociones',
        title: 'Cupones · Panel Nalu',
        loadComponent: () => import('./admin/promociones/promociones').then((m) => m.Promociones),
      },
      {
        path: 'materiales',
        title: 'Materiales · Panel Nalu',
        loadComponent: () => import('./admin/materiales/materiales').then((m) => m.Materiales),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
