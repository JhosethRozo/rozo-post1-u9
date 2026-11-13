# Verificación de checkpoints y rúbrica

Las capturas de comandos corresponden a un visor HTML de los logs y del código real.
JaCoCo y GitHub Actions se capturan directamente desde sus páginas. Los logs conservan
la salida de las ejecuciones; los nombres de las rutas pueden reflejar la codificación de
la consola de Windows, sin afectar la compilación ni las aserciones.

| Paso | Comprobación | Evidencia |
| --- | --- | --- |
| 1 | `mvn compile`, JDK 21 y Spring Boot 3.3.x; BUILD SUCCESS. | [Captura](evidencias/01-compilacion.png), [log](evidencias/01-compile.log) |
| 2 | Entidad Cita, enum EMAIL/SMS y `JpaRepository<Cita, Long>` con consulta derivada. | [Captura del código](evidencias/02-dominio.png) |
| 3 | Exactamente dos implementaciones Strategy con `@Component`; Factory recibe ambas por constructor sin `new`. | [Captura del código](evidencias/03-strategy-factory.png) |
| 4 | Servicio con dependencias por constructor; arranque con H2 vacía y Tomcat en 8080. | [Captura](evidencias/04-arranque.png), [log](evidencias/04-arranque.log) |
| 5 | Decisiones escritas antes de las pruebas; tres pruebas iniciales pasan, incluida ventana exacta. | [Pruebas](evidencias/05-parte1-pruebas.png), [log](evidencias/05-parte1-test.log) |
| 6 | Proyecto en raíz y cuatro commits de Parte 1, con decisiones antes de la suite. | [Historial y decisiones](evidencias/06-parte1-git.png) |
| 7 | Versión deliberadamente acoplada compila; diagnóstico registrado antes del refactor. | [Antes del refactor](evidencias/07-antes-dip.png), [log](evidencias/07-antes-refactor.log) |
| 8 | Bean urgente en `@Configuration`, abstracción con qualifier por constructor, sin construcción del canal/gateway en el servicio. Seis unitarias pasan. | [Después y pruebas](evidencias/08-despues-dip.png), [log](evidencias/08-refactor-test.log) |
| 9 | `@DataJpaTest` con H2 automática, exactamente un resultado en el escenario solicitado; límites inclusivos comprobados adicionalmente. | [Captura](evidencias/09-jpa.png) |
| 10 | `@SpringBootTest`, reloj fijo `@Primary` y gateway `@MockBean`; persistencia y notificación verificadas. | [Captura y resumen](evidencias/10-integracion.png), [mvn clean verify](evidencias/10-verify.log) |
| 11 | Workflow en `.github/workflows/ci.yml`, JDK 21, `mvn verify`, H2 sin servicios externos. | [Workflow](../.github/workflows/ci.yml), [Actions](https://github.com/JhosethRozo/rozo-post1-u9/actions) |
| 12 | README consolidado, badge, decisiones y antes/después; más de tres commits en Parte 2. | [Historial y archivos](evidencias/12-entrega.png), [README](../README.md) |
| Cobertura | JaCoCo generado en fase verify; servicio con 100 % de instrucciones y ramas. | [General](evidencias/13-cobertura.png), [Servicio](evidencias/14-cobertura-servicio.png) |

## Correspondencia con la rúbrica

| Criterio | Comprobación entregada |
| --- | --- |
| Implementación, 40 % | Mocks para repositorio/Factory/estrategias, Clock.fixed explícito, refactor DIP, JPA/H2 y gateway con @MockBean. |
| Funcionalidad, 25 % | 11 pruebas pasan: seis unitarias, dos JPA y tres del contexto completo; fallo urgente conserva estado pendiente. |
| Documentación, 20 % | README con decisiones por colaborador, patrón → prueba, antes/después, comandos, capturas, reporte y badge. |
| Estilo y convenciones, 15 % | Nombres metodo_condicion_resultado, paquetes de prueba paralelos a los principales y composición en config. |

## Reproducir las capturas

Ejecutar `mvn verify` para generar Surefire y JaCoCo. Instalar Playwright en un entorno Python
y ejecutar `python scripts/capturar_evidencias.py`. El script utiliza Chrome instalado en Windows;
`--chrome` permite indicar otro ejecutable Chromium. Los logs de Parte 1 se conservan como
evidencia histórica y no se reemplazan por una ejecución del estado final.
La opción `--workflow-url URL` añade una captura directa de la ejecución pública de Actions.
