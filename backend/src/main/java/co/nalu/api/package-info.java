/**
 * Capa de entrada HTTP: controladores REST, lectura del JWT, validación de entrada con YAVI y
 * traducción de excepciones a {@code application/problem+json}.
 *
 * <p>Solo depende de {@code negocio}. Nunca usa entidades ni repositorios de {@code datos}.
 */
package co.nalu.api;
