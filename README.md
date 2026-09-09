# Monet 💰

**Aplicación de finanzas personales nativa para Android**  
Kotlin + Jetpack Compose + Material 3

---

## Descarga

> **[⬇ Descargar Monet-v1.0.apk](https://github.com/SaulEsca99/Monet/releases/download/v1.0/Monet-v1.0.apk)**

Instala el APK en tu Android directamente. Requiere Android 8.0+ (API 26).

---

## Características

### 💳 Cuentas y saldos
- Múltiples cuentas (efectivo, débito)
- Ajuste manual de saldos
- Saldo total con sparkline animada

### 💸 Movimientos
- Registro de ingresos y gastos
- Categorías con emojis (Comida, Transporte, Salud, Ropa, Tecnología, Hogar, etc.)
- Filtro por mes y categoría
- Swipe para eliminar
- Tipos de ingreso personalizados
- Crear billetera directamente desde el formulario

### 🔄 Suscripciones recurrentes
- Netflix, Spotify, Gym, etc.
- Contador de días hasta próximo cobro
- Historial de pagos
- Pago con descuento de cuenta
- Estadísticas de meses pagados

### 🏷 MSI — Compras a Meses Sin Intereses
- Registro de planes MSI
- Mensualidad calculada automáticamente
- Progreso visual (barra animada)
- Meses pre-pagados antes de registrar
- Pago mensual con descuento de cuenta / sin descontar
- Liquidación anticipada
- Días hasta próximo cobro (como suscripciones)
- Indica cuándo ya pagaste el mes actual

### 💵 Préstamos
- Seguimiento de préstamos prestados
- Fecha de devolución esperada
- Alertas de préstamos vencidos
- Marcar como cobrado

### 📊 Dashboard (Inicio)
- Saldo total con sparkline en tiempo real
- PAGOS/MES: solo compromisos pendientes de pago este mes
- LIBRE: dinero real disponible tras compromisos
- Anillos de progreso (MSI y Suscripciones)
- Gráfica de tendencia 6 meses (ingresos vs gastos)
- Donut de gastos por categoría
- Alertas de préstamos vencidos / subs urgentes
- DEUDAS A FUTURO: MSI restante total + préstamos (separado del saldo)
- Transacciones recientes

### 🌗 Tema claro/oscuro
- Material 3 con paleta personalizada
- Toggle en el top bar global

---

## Stack técnico

| Tecnología | Uso |
|---|---|
| Kotlin | Lenguaje principal |
| Jetpack Compose | UI declarativa |
| Material 3 | Design system |
| DataStore Preferences | Persistencia local (JSON) |
| kotlinx.serialization | Serialización de datos |
| ViewModel + StateFlow | Estado reactivo |
| Canvas API | Gráficas (sparkline, donut, barras) |

---

## Instalación desde código

```bash
git clone https://github.com/TU_USUARIO/Monet.git
cd Monet

# Configurar SDK en local.properties
echo "sdk.dir=/tu/ruta/android/sdk" > local.properties

# Compilar
./gradlew assembleDebug

# Instalar en dispositivo conectado
adb install app/build/outputs/apk/debug/app-debug.apk
```

Requiere JDK 17 y Android SDK 34.

---

## Estructura del proyecto

```
app/src/main/java/com/mifinanza/app/
├── data/
│   ├── Models.kt          # Modelos de datos + helpers
│   └── Repository.kt      # DataStore + lógica de negocio
├── viewmodel/
│   └── AppViewModel.kt    # Estado global reactivo
├── ui/
│   ├── Navigation.kt      # NavHost + TopBar premium
│   ├── screens/
│   │   ├── DashboardScreen.kt
│   │   ├── MovimientosScreen.kt
│   │   ├── SuscripcionesScreen.kt
│   │   ├── MSIScreen.kt
│   │   ├── PrestamosScreen.kt
│   │   ├── CuentasScreen.kt
│   │   └── WelcomeScreen.kt
│   ├── components/
│   │   ├── Cards.kt           # Componentes reutilizables
│   │   ├── Charts.kt          # Gráficas (sparkline, donut, barras)
│   │   └── NotificationBanner.kt  # Notificaciones in-app
│   └── theme/
│       └── Theme.kt
```

---

## Licencia

MIT — uso personal libre.
