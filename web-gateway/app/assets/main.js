$().ready(function () {
    $(".log-in-as").each(function() {
        var logInAs = $(this);
        var userId = logInAs.data("user-id");
        logInAs.click(function() {
            $.ajax({
                type: "POST",
                url: "/currentuser/" + userId
            }).then(function() {
                window.location.reload();
            });
        });
    });
});