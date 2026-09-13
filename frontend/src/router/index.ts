import { createRouter, createWebHistory } from "vue-router";
import OverviewView from "../views/OverviewView.vue";
import SessionListView from "../views/SessionListView.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", name: "overview", component: OverviewView, meta: { label: "运营总览" } },
    { path: "/sessions", name: "sessions", component: SessionListView, meta: { label: "场次报名" } },
  ],
});

export default router;
