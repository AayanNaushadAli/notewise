import type { FastifyPluginAsync } from "fastify";
import { z } from "zod";
import { prisma } from "../prisma.js";

const CreateNotebookSchema = z.object({
  title: z.string().min(1),
  coverColor: z.string().optional(),
  folderId: z.string().uuid().optional(),
});

export const notebookRoutes: FastifyPluginAsync = async (fastify) => {
  fastify.addHook("onRequest", async (request, reply) => {
    try {
      await request.jwtVerify();
    } catch (err) {
      reply.status(401).send({ error: "Unauthorized" });
    }
  });

  fastify.get("/", async (request, reply) => {
    const user = request.user as { id: string };
    const notebooks = await prisma.notebook.findMany({
      where: { userId: user.id, isDeleted: false },
      include: {
        pages: {
          where: { isDeleted: false },
          orderBy: { pageIndex: "asc" },
          take: 1, // First page preview
        },
      },
      orderBy: { updatedAt: "desc" },
    });
    return reply.send({ notebooks });
  });

  fastify.post("/", async (request, reply) => {
    const user = request.user as { id: string };
    const parse = CreateNotebookSchema.safeParse(request.body);
    if (!parse.success) {
      return reply.status(400).send({ error: parse.error.format() });
    }

    const { title, coverColor, folderId } = parse.data;
    const notebook = await prisma.notebook.create({
      data: {
        userId: user.id,
        title,
        coverColor: coverColor || "#1E3A8A",
        folderId,
        pages: {
          create: {
            pageIndex: 0,
            templateType: "ruled",
          },
        },
      },
      include: { pages: true },
    });

    return reply.status(201).send({ notebook });
  });

  fastify.get("/:id", async (request, reply) => {
    const user = request.user as { id: string };
    const { id } = request.params as { id: string };

    const notebook = await prisma.notebook.findFirst({
      where: { id, userId: user.id, isDeleted: false },
      include: {
        pages: {
          where: { isDeleted: false },
          orderBy: { pageIndex: "asc" },
        },
      },
    });

    if (!notebook) {
      return reply.status(404).send({ error: "Notebook not found" });
    }

    return reply.send({ notebook });
  });

  fastify.delete("/:id", async (request, reply) => {
    const user = request.user as { id: string };
    const { id } = request.params as { id: string };

    await prisma.notebook.updateMany({
      where: { id, userId: user.id },
      data: { isDeleted: true },
    });

    return reply.send({ success: true });
  });
};

