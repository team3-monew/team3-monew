import axios from "axios";

export const http = axios.create({
  baseURL: "/api",
  headers: { "Content-Type": "application/json" },
  paramsSerializer: {
    indexes: null,
  },
});

if (import.meta.env.DEV) {
  http.interceptors.request.use((cfg) => {
    console.log(
      `[REQ] ${cfg.method?.toUpperCase()} ${cfg.baseURL}${cfg.url}`,
      cfg.data,
    );
    return cfg;
  });
  http.interceptors.response.use(
    (res) => {
      console.log(`[RES] ${res.status} ${res.config.url}`, res.data);
      return res;
    },
    (err) => {
      console.warn(
        `[ERR] ${err?.response?.status} ${err?.config?.url}`,
        err?.response?.data,
      );
      return Promise.reject(err);
    },
  );
}
