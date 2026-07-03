import "react-toastify/dist/ReactToastify.css";
import { Slide, ToastContainer } from "react-toastify";

export default function Toast() {
  return (
    <ToastContainer
      hideProgressBar={true}
      closeButton={false}
      limit={2}
      position="bottom-center"
      autoClose={3000}
      newestOnTop={true}
      transition={Slide}
      draggable
      pauseOnHover
      pauseOnFocusLoss
    />
  );
}
