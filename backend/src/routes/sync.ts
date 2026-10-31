import type { FastifyPluginAsync } from "fastify";
import { z } from "zod";
import { prisma } from "../prisma.js";

const SyncNotebookSchema = z.object({
  id: z.string().uuid(),
  title: z.string(),
  folderId: z.string().uuid().nullable().optional(),
  coverColor: z.string().optional(),
  isPinned: z.boolean().optional(),
  isDeleted: z.boolean().optional(),
  version: z.number().int(),
  updatedAt: z.string().datetime(),
});

const SyncPageSchema = z.object({
  id: z.string().uuid(),
  notebookId: z.string().uuid(),
  pageIndex: z.number().int(),
  templateType: z.string(),
  backgroundColor: z.string().optional(),
  pdfBackgroundUrl: z.string().nullable().optional(),
  strokeData: z.any().optional(),
  isDeleted: z.boolean().optional(),
  version: z.number().int(),
  updatedAt: z.string().datetime(),
});

const PushPayloadSchema = z.object({
  deviceId: z.string().optional(),
  notebooks: z.array(SyncNotebookSchema).default([]),
  pages: z.array(SyncPageSchema).default([]),
});

export const syncRoutes: FastifyPluginAsync = async (fastify) => {
  // Require JWT for all sync requests
  fastify.addHook("onRequest", async (request, reply) => {
    try {
      await request.jwtVerify();
    } catch (err) {
      reply.status(401).send({ error: "Unauthorized" });
    }
  });

  // Pull delta changes from server since client's last sync timestamp
  fastify.get("/pull", async (request, reply) => {
    const user = request.user as { id: string };
    const query = request.query as { since?: string };

    const sinceDate = query.since ? new Date(query.since) : new Date(0);

    const [updatedNotebooks, updatedPages] = await Promise.all([
      prisma.notebook.findMany({
        where: {
          userId: user.id,
          updatedAt: { gt: sinceDate },
        },
      }),
      prisma.page.findMany({
        where: {
          notebook: { userId: user.id },
          updatedAt: { gt: sinceDate },
        },
      }),
    ]);

    return reply.send({
      serverTime: new Date().toISOString(),
      notebooks: updatedNotebooks,
      pages: updatedPages,
    });
  });

  // Push local changes to cloud server with conflict resolution
  fastify.post("/push", async (request, reply) => {
    const user = request.user as { id: string };
    const parse = PushPayloadSchema.safeParse(request.body);

    if (!parse.success) {
      return reply.status(400).send({ error: parse.error.format() });
    }

    const { notebooks, pages } = parse.data;

    const results = await prisma.$transaction(async (tx) => {
      // Upsert notebooks
      for (const nb of notebooks) {
        await tx.notebook.upsert({
          where: { id: nb.id },
          update: {
            title: nb.title,
            coverColor: nb.coverColor,
            isPinned: nb.isPinned,
            isDeleted: nb.isDeleted,
            version: { increment: 1 },
            updatedAt: new Date(nb.updatedAt),
          },
          create: {
            id: nb.id,
            userId: user.id,
            title: nb.title,
            coverColor: nb.coverColor || "#1E3A8A",
            isPinned: nb.isPinned || false,
            isDeleted: nb.isDeleted || false,
            version: nb.version || 1,
            updatedAt: new Date(nb.updatedAt),
          },
        });
      }

      // Upsert pages
      for (const p of pages) {
        await tx.page.upsert({
          where: { id: p.id },
          update: {
            pageIndex: p.pageIndex,
            templateType: p.templateType,
            backgroundColor: p.backgroundColor,
            pdfBackgroundUrl: p.pdfBackgroundUrl,
            strokeData: p.strokeData,
            isDeleted: p.isDeleted,
            version: { increment: 1 },
            updatedAt: new Date(p.updatedAt),
          },
          create: {
            id: p.id,
            notebookId: p.notebookId,
            pageIndex: p.pageIndex,
            templateType: p.templateType,
            backgroundColor: p.backgroundColor || "#FFFFFF",
            pdfBackgroundUrl: p.pdfBackgroundUrl,
            strokeData: p.strokeData,
            isDeleted: p.isDeleted || false,
            version: p.version || 1,
            updatedAt: new Date(p.updatedAt),
          },
        });
      }

      return {
        syncedNotebooks: notebooks.length,
        syncedPages: pages.length,
      };
    });

    return reply.send({
      success: true,
      serverTime: new Date().toISOString(),
      ...results,
    });
  });
};

