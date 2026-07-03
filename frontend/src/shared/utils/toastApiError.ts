import { toast } from "react-toastify";
import { normalizeError } from "@/shared/lib/normalizeError";

export const toastApiError = (error: unknown) => {
  toast.error(normalizeError(error).message);
};
