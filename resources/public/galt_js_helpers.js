window.galt = {
  copyInputToClipboard: function(input) {
    input.select();
    navigator.clipboard.writeText(input.value);
    input.blur();
  }
}
