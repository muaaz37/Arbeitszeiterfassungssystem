const messages = {
  session: 'Deine Anmeldung ist ungültig oder abgelaufen. Bitte melde dich erneut an.',
  forbidden: 'Du hast keine Berechtigung für diesen Zugriff.',
  network: 'Die Backend-API ist momentan nicht erreichbar.',
} as const;

export class ApiAccessError extends Error {
  constructor(readonly kind: keyof typeof messages) {
    super(messages[kind]);
  }
}
