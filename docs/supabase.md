# Preparar Supabase

Lo que hay que hacer una vez en el panel, antes de escribir código.

## 1. Crear el proyecto

1. Entrar en [supabase.com](https://supabase.com) y crear una cuenta. El plan
   gratuito basta.
2. New project. Elegir organización, nombre y región.
3. **Guardar la contraseña de la base de datos** que pide ahí. No se puede
   volver a ver, y hace falta para conectarse por SQL.
4. Esperar al aprovisionamiento, que tarda un par de minutos.

## 2. Copiar las dos claves

Project Settings → API:

- **Project URL**, algo como `https://abcdefgh.supabase.co`
- **La clave pública** (publishable, antes llamada `anon`)

La clave de servicio (`service_role`) **no se usa en una app**: se puede
extraer del binario y salta las reglas de seguridad por filas.

## 3. Ajustar el correo para poder probar

Authentication → Providers → Email:

- **Desactivar «Confirm email»** mientras se desarrolla. Con la confirmación
  activada, el registro devuelve una sesión nula y hay que abrir un enlace que
  apunta a `Site URL`, que por defecto es `localhost:3000`.
- En producción se vuelve a activar, y hace falta un SMTP propio: el correo
  integrado de Supabase tiene un límite bajo por hora y en proyectos nuevos solo
  entrega a direcciones del equipo.

## 4. Bajar la caducidad del token para probar el refresco

Authentication → Sessions → JWT expiry: ponerlo en **60 segundos**. Con la hora
por defecto, comprobar que el refresco funciona exigiría esperar una hora.

## 5. Comprobar con curl, antes de escribir Kotlin

```bash
SUPABASE_URL=https://abcdefgh.supabase.co
SUPABASE_KEY=sb_publishable_...

# Registro
curl -s -X POST "$SUPABASE_URL/auth/v1/signup" \
  -H "apikey: $SUPABASE_KEY" -H "Content-Type: application/json" \
  -d '{"email":"prueba@ejemplo.com","password":"contrasena-larga"}'

# Entrar
curl -s -X POST "$SUPABASE_URL/auth/v1/token?grant_type=password" \
  -H "apikey: $SUPABASE_KEY" -H "Content-Type: application/json" \
  -d '{"email":"prueba@ejemplo.com","password":"contrasena-larga"}'

# Refrescar
curl -s -X POST "$SUPABASE_URL/auth/v1/token?grant_type=refresh_token" \
  -H "apikey: $SUPABASE_KEY" -H "Content-Type: application/json" \
  -d '{"refresh_token":"..."}'
```

Si algo falla aquí, falla en la app. Depurar una petición con curl cuesta
segundos; depurarla dentro de un cliente de Ktor, minutos.

## Una advertencia de calendario

Los proyectos gratuitos **se pausan tras siete días sin actividad**. Quien deje
el curso dos semanas vuelve a un proyecto pausado y tiene que restaurarlo desde
el panel. No es un fallo de la app.
