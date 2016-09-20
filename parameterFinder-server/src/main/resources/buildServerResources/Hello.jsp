<html>
    <head>
        <script>
            window.addEventListener('load', function () {
                var xhr = new XMLHttpRequest();
                xhr.open('GET', '/plugins/parameterFinder/parameters.xml');
                xhr.addEventListener('load', function () {

                    // Iterate over parameter elements
                    Array.from(this.responseXML.getElementsByTagName('parameter')).forEach(function (parameter) {

                        // Iterate over parameter children
                        Array.from(parameter.children).forEach(function (child) {
                            document.getElementById('params').innerHTML += child.nodeName + ': ' + child.innerHTML + '<br>';
                        });

                        document.getElementById('params').innerHTML += '<br>';
                    });
                });
                xhr.send();
            });
        </script>
    </head>
    <body>
        <div id="main">
            <div id="params" style="padding:30px;"></div>
        </div>
    </body>
</html>
