// Renders an SVG to an opaque square sRGB PNG with macOS's own SVG support (App Store icons may not have alpha).
// Usage: swift design/render-png.swift <in.svg> <out.png> <size>
import AppKit
import UniformTypeIdentifiers

let args = CommandLine.arguments
guard args.count == 4, let size = Int(args[3]), let image = NSImage(contentsOfFile: args[1]) else {
    FileHandle.standardError.write("usage: render-png.swift <in.svg> <out.png> <size>\n".data(using: .utf8)!)
    exit(1)
}
// noneSkipLast: an RGB context with no alpha channel, so the PNG is written without one.
let context = CGContext(
    data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: 0,
    space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue
)!
NSGraphicsContext.saveGraphicsState()
NSGraphicsContext.current = NSGraphicsContext(cgContext: context, flipped: false)
image.draw(in: NSRect(x: 0, y: 0, width: size, height: size))
NSGraphicsContext.restoreGraphicsState()
let destination = CGImageDestinationCreateWithURL(URL(fileURLWithPath: args[2]) as CFURL, UTType.png.identifier as CFString, 1, nil)!
CGImageDestinationAddImage(destination, context.makeImage()!, nil)
guard CGImageDestinationFinalize(destination) else { exit(1) }
