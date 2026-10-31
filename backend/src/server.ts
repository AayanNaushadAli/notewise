import Fastify from "fastify";
import cors from "@fastify/cors";
import jwt from "@fastify/jwt";
import dotenv from "dotenv";
import { authRoutes } from "./routes/auth.js";
import { syncRoutes } from "./routes/sync.js";
import { notebookRoutes } from "./routes/notebooks.js";

dotenv.config();

const fastify = Fastify({
  logger: {
    level: process.env.LOG_LEVEL || "info",
  },
});

await fastify.register(cors, {
  origin: true,
  methods: ["GET", "POST", "PUT", "DELETE", "PATCH"],
});

await fastify.register(jwt, {
  secret: process.env.JWT_SECRET || "notewise_development_jwt_secret_key_12345",
});

// Health check
fastify.get("/health", async () => {
  return { status: "ok", timestamp: new Date().toISOString() };
});

// API Routes
await fastify.register(authRoutes, { prefix: "/api/auth" });
await fastify.register(syncRoutes, { prefix: "/api/sync" });
await fastify.register(notebookRoutes, { prefix: "/api/notebooks" });

const port = Number(process.env.PORT) || 3000;
const host = process.env.HOST || "0.0.0.0";

try {
  await fastify.listen({ port, host });
  fastify.log.info(`Notewise Cloud Sync Server running at http://${host}:${port}`);
} catch (err) {
  fastify.log.error(err);
  process.exit(1);
}

