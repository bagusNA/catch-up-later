/**
 * Protects the application routes and routes first-run instances to setup.
 *
 * Runs client-side (the app is rendered as an SPA) after the auth plugin has
 * initialized the session.
 */
export default defineNuxtRouteMiddleware(async (to) => {
  const auth = useAuth()
  await auth.initialize()

  const publicRoutes = ['/login', '/setup']
  const isPublic = publicRoutes.includes(to.path)

  if (auth.setupRequired.value) {
    return to.path === '/setup' ? undefined : navigateTo('/setup')
  }

  if (to.path === '/setup') {
    return navigateTo(auth.user.value ? '/library' : '/login')
  }

  if (isPublic) {
    if (to.path === '/login' && auth.user.value) {
      return navigateTo('/library')
    }
    return undefined
  }

  if (!auth.user.value) {
    return navigateTo('/login')
  }

  if (to.path === '/') {
    return navigateTo('/library')
  }

  return undefined
})
