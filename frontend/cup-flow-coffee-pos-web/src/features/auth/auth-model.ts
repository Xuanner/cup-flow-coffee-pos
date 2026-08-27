import { z } from "zod";

export const currentUserSchema = z.object({
  id: z.string().min(1),
  displayName: z.string().min(1),
  roles: z.array(z.string().min(1)).min(1),
  defaultPath: z.enum(["/pos", "/dashboard"]),
});

export type CurrentUser = z.infer<typeof currentUserSchema>;

export interface LoginCredentials {
  username: string;
  password: string;
}
