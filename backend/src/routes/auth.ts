import type { FastifyPluginAsync } from "fastify";
import { z } from "zod";
import { prisma } from "../prisma.js";

const AuthBodySchema = z.object({
  email: z.string().email(),
  password: z.string().min(6),
  name: z.string().optional(),
});

export const authRoutes: FastifyPluginAsync = async (fastify) => {
  fastify.post("/register", async (request, reply) => {
    const parse = AuthBodySchema.safeParse(request.body);
    if (!parse.success) {
      return reply.status(400).send({ error: parse.error.format() });
    }

    const { email, password, name } = parse.data;

    const existing = await prisma.user.findUnique({ where: { email } });
    if (existing) {
      return reply.status(409).send({ error: "User already exists" });
    }

    // In production, hash password using bcrypt/argon2
    const user = await prisma.user.create({
      data: {
        email,
        name: name || email.split("@")[0],
        passwordHash: password,
      },
    });

    const token = fastify.jwt.sign({ id: user.id, email: user.email });
    return reply.status(201).send({ token, user: { id: user.id, email: user.email, name: user.name } });
  });

  fastify.post("/login", async (request, reply) => {
    const parse = AuthBodySchema.pick({ email: true, password: true }).safeParse(request.body);
    if (!parse.success) {
      return reply.status(400).send({ error: parse.error.format() });
    }

    const { email, password } = parse.data;
    const user = await prisma.user.findUnique({ where: { email } });
    if (!user || user.passwordHash !== password) {
      return reply.status(401).send({ error: "Invalid email or password" });
    }

    const token = fastify.jwt.sign({ id: user.id, email: user.email });
    return reply.send({ token, user: { id: user.id, email: user.email, name: user.name } });
  });
};

