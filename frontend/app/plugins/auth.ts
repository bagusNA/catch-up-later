/**
 * Initializes authentication state once, before the first route renders.
 */
export default defineNuxtPlugin(async () => {
  const auth = useAuth()
  await auth.initialize()
})
