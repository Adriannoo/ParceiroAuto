export interface User {
  id: number;
  login: string;
  name: string | null;
}

export interface UserRegistration {
  name: string;
  email: string;
  password: string;
}
