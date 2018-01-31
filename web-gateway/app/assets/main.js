$().ready(function () {
    $(".log-in-as").each(function() {
        var logInAs = $(this);
        var userId = logInAs.data("user-id");
        var csrfToken = logInAs.data("csrf-token");
        logInAs.click(function() {
            $.ajax({
                type: "POST",
                url: "/currentuser/" + userId,
                headers: {
                    "CSRF-Token" : csrfToken
                }
            }).then(function() {
                window.location.reload();
            });
        });
    });
});
$(document).foundation();
