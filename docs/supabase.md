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

## 6. La función para eliminar la cuenta

Supabase **no expone** un endpoint que borre tu propio usuario. Tiene sentido:
sería dar a cualquier token la capacidad de borrar filas de una tabla del
sistema. La vía documentada es una función SQL con `security definer`, que se
ejecuta con los permisos de quien la creó pero comprueba quién la llama.

Se pega en el SQL Editor del panel y se ejecuta una vez:

```sql
create or replace function public.delete_user()
returns void
language plpgsql
security definer
-- `set search_path` no es adorno: sin él, una función `security definer` puede
-- ser engañada para ejecutar código de otro esquema con permisos elevados.
set search_path = ''
as $$
begin
  if auth.uid() is null then
    raise exception 'no hay sesión';
  end if;
  delete from auth.users where id = auth.uid();
end;
$$;

-- Solo quien ha iniciado sesión. `anon` no, o cualquiera con la clave pública
-- podría llamarla.
revoke all on function public.delete_user() from public, anon;
grant execute on function public.delete_user() to authenticated;
```

La app la llama con `POST /rest/v1/rpc/delete_user` y la cabecera `Authorization`
del usuario. Es otra API del mismo proyecto —`/rest/v1/`, no `/auth/v1/`—, así
que la URL va completa.

**Esto no es opcional.** Si la app deja crear una cuenta, tanto App Store como
Play exigen que deje borrarla desde dentro. Sin esta opción, la revisión la
rechaza.

## 7. El correo de recuperación

La app manda `POST /auth/v1/recover` con `redirect_to=pokedex://auth/callback`.
Para que Supabase acepte esa dirección hay que añadirla en **Authentication →
URL Configuration → Redirect URLs**; las que no estén en la lista se ignoran y
el usuario acaba en el «Site URL», que por defecto es `localhost`.

El enlace del correo vuelve con los tokens en el **fragmento** (`#`), no en la
consulta (`?`). Es deliberado: el fragmento no se envía al servidor, así que los
tokens no quedan en el registro de ningún servidor por el que pase el enlace.
