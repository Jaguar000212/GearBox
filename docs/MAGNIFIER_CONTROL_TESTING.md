# Magnifier asynchronous control regression

Device execution is still required. The source fix adds independent zoom and torch
request generations, alongside the existing camera epoch and bound-camera checks.
Cleanup invalidates both generations. A superseded completion must not alter UI
state or report a camera error; a failure for the current request still does.

- Start Magnifier on a camera with zoom support. Drag the zoom slider rapidly in
  both directions, including endpoints. The preview must remain live, with the
  final successful requested zoom reflected in the UI. Superseded CameraX zoom
  futures may fail normally and must not stop the camera.
- On a flash-capable device, tap torch repeatedly before its first request
  completes. Old completions must not overwrite the latest successful torch
  state or stop the preview. Confirm the actual torch state agrees with the UI.
- While zoom or torch work is pending, Freeze, Stop, background, or leave the
  screen. Preview/torch must stop; delayed completions must not restore a live
  state, discard the frozen frame, or report an error in a later camera session.
- Start again after stopping. Verify controls work for the new session.
- Exercise an actual current-request failure (for example camera interruption
  on a suitable test device). It must still clean up and display the error;
  generation fencing must not suppress failures of the newest active request.

No mocked or device control test has been executed for this change. A test that
only duplicates the token comparison would not verify CameraX callback behavior.
