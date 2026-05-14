# Regulus Documentation

User-facing documentation for the Regulus platform, built with [MkDocs Material](https://squidfunk.github.io/mkdocs-material/).

**Live site:** [regulus.neullabs.com](https://regulus.neullabs.com)

## Quick Start

### Prerequisites

- Python 3.8+
- pip

### Installation

```bash
# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
```

### Development Server

```bash
# Start the local development server
mkdocs serve

# Visit http://localhost:8000
```

### Building

```bash
# Build static site
mkdocs build

# Output in ./site directory
```

## Structure

```
documentation/
├── mkdocs.yml              # MkDocs configuration
├── requirements.txt        # Python dependencies
├── README.md              # This file
└── docs/
    ├── index.md           # Home page
    ├── stylesheets/       # Custom CSS
    ├── getting-started/   # Getting started guides
    ├── guides/            # Feature guides
    │   ├── llm-providers/ # LLM provider setup
    │   ├── core-features/ # Core feature docs
    │   ├── integration/   # Integration guides
    │   └── operations/    # Operations guides
    ├── compliance/        # Compliance documentation
    ├── api-reference/     # API documentation
    └── advanced/          # Advanced topics
```

## Writing Documentation

### Adding Pages

1. Create a new `.md` file in the appropriate directory
2. Add the page to the `nav` section in `mkdocs.yml`

### Markdown Extensions

This documentation uses several Markdown extensions:

- **Admonitions**: `!!! note`, `!!! warning`, `!!! danger`
- **Code highlighting**: ` ```python ` with language specification
- **Tabs**: `=== "Tab 1"` for tabbed content
- **Tables**: Standard Markdown tables
- **Task lists**: `- [ ]` and `- [x]`

### Code Examples

Use fenced code blocks with language specification:

````markdown
```java
public class Example {
    // Your code here
}
```
````

### Admonitions

```markdown
!!! note "Optional Title"
    This is a note admonition.

!!! warning
    This is a warning.

!!! danger "Important"
    This is a danger/critical admonition.
```

## Deployment

The documentation is published at [regulus.neullabs.com](https://regulus.neullabs.com).

### GitHub Pages

```bash
mkdocs gh-deploy
```

### Manual Deployment

```bash
mkdocs build
# Copy ./site to your web server
```

## Contributing

1. Create a feature branch
2. Make your changes
3. Test locally with `mkdocs serve`
4. Submit a pull request

## License

See the main repository LICENSE file.
